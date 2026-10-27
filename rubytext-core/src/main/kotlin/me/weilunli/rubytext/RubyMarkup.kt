package me.weilunli.rubytext

/**
 * Plain-text ruby markup.
 *
 * ## Brace markup ([parse])
 *
 * | Markup                | Meaning                                                    |
 * |-----------------------|------------------------------------------------------------|
 * | `{漢字|かんじ}`        | one reading for the whole base                             |
 * | `{東京|とう|きょう}`   | one reading per character (count must match)              |
 * | `{你好|ㄋㄧˇ ㄏㄠˇ}`   | whitespace-separated readings, one per character           |
 * | `{你好|nǐ hǎo}`        | same, Pinyin                                               |
 * | `{北京大学|Peking University}` | word count ≠ character count: one reading for the whole base |
 * | `\{ \} \| \\`          | literal `{ } | \`; `\ ` is a space that does not split readings |
 *
 * A `{` that is not closed, or a group without `|`, is kept as literal text.
 */
public object RubyMarkup {
    private const val ESCAPED_SPACE = ''
    private val whitespace = Regex("\\s+")

    /** Parses brace markup. Never throws: malformed groups stay literal text. */
    public fun parse(markup: String): RubyString {
        val out = SegmentSink()
        var i = 0
        while (i < markup.length) {
            val c = markup[i]
            when {
                c == '\\' && i + 1 < markup.length -> {
                    out.plain(markup[i + 1].toString()); i += 2
                }
                c == '{' -> {
                    val group = readGroup(markup, i)
                    if (group == null) {
                        out.plain("{"); i++
                    } else {
                        out.segment(group.first)
                        i = group.second
                    }
                }
                else -> {
                    out.plain(c.toString()); i++
                }
            }
        }
        return out.build()
    }

    /** Reads `{base|r...}` starting at [start]. Returns the segment and the index after `}`. */
    private fun readGroup(s: String, start: Int): Pair<RubySegment, Int>? {
        val parts = ArrayList<String>()
        val cur = StringBuilder()
        var i = start + 1
        while (i < s.length) {
            val c = s[i]
            when (c) {
                '\\' -> {
                    if (i + 1 >= s.length) return null
                    val next = s[i + 1]
                    cur.append(if (next == ' ') ESCAPED_SPACE else next)
                    i += 2
                    continue
                }
                '{' -> return null
                '|' -> { parts += cur.toString(); cur.setLength(0) }
                '}' -> {
                    parts += cur.toString()
                    if (parts.size < 2 || parts[0].isEmpty()) return null
                    return makeSegment(parts[0], parts.subList(1, parts.size)) to i + 1
                }
                else -> cur.append(c)
            }
            i++
        }
        return null
    }

    private fun makeSegment(rawBase: String, rawReadings: List<String>): RubySegment {
        val base = rawBase.unescapeSpace()
        val count = Graphemes.count(base)
        if (rawReadings.size > 1) {
            val readings = rawReadings.map { it.trim().unescapeSpace() }
            return if (readings.size == count) RubySegment(base, readings)
            else RubySegment(base, readings.joinToString(""))
        }
        val reading = rawReadings[0].trim()
        if (count > 1 && whitespace.containsMatchIn(reading)) {
            val words = reading.split(whitespace)
            if (words.size == count) return RubySegment(base, words.map { it.unescapeSpace() })
        }
        return RubySegment(base, reading.unescapeSpace())
    }

    private fun String.unescapeSpace() = replace(ESCAPED_SPACE, ' ')

    /** Formats [text] as brace markup that [parse] reads back unchanged. */
    public fun format(text: RubyString): String = buildString {
        for (s in text.segments) {
            if (!s.hasRuby) {
                appendEscaped(s.base, escapeSpace = false)
                continue
            }
            append('{')
            appendEscaped(s.base, escapeSpace = false)
            if (s.readings.size == 1) {
                append('|')
                // A spaced reading over a multi-character base would be re-read as one per character.
                appendEscaped(s.readings[0], escapeSpace = s.characters.size > 1)
            } else {
                for (r in s.readings) {
                    append('|'); appendEscaped(r, escapeSpace = true)
                }
            }
            append('}')
        }
    }

    private fun StringBuilder.appendEscaped(s: String, escapeSpace: Boolean) {
        for (c in s) {
            when {
                c == '{' || c == '}' || c == '|' || c == '\\' -> append('\\').append(c)
                c == ' ' && escapeSpace -> append("\\ ")
                else -> append(c)
            }
        }
    }

    /**
     * Parses the v1 `app:combinedText` format, for migrating from `RubyTextView`:
     * space-separated tokens, `base|reading` for annotated tokens, `base|-` for no reading.
     * Spaces between tokens are dropped, as v1 did.
     */
    public fun parseLegacy(combinedText: String): RubyString {
        val out = SegmentSink()
        for (token in combinedText.split(' ')) {
            if (token.isEmpty()) continue
            val parts = token.split('|').dropLastWhile { it.isEmpty() }
            if (parts.size == 2 && parts[0].isNotEmpty()) {
                out.segment(RubySegment(parts[0], parts[1].takeUnless { it == "-" }))
            } else {
                out.plain(parts.firstOrNull() ?: "")
            }
        }
        return out.build()
    }

    /**
     * Parses Aozora Bunko ruby (青空文庫):
     * `｜青空《あおぞら》` marks the base explicitly; `漢字《かんじ》` takes the run of kanji
     * (including 々〆〇ヶ) before `《` as the base. Other Aozora notation is left as is.
     */
    public fun parseAozora(text: String): RubyString {
        val out = SegmentSink()
        val buf = StringBuilder()
        var mark = -1 // index in buf where ｜ was seen

        fun restoreMark() {
            if (mark >= 0) { buf.insert(mark, '｜'); mark = -1 }
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '｜' -> { restoreMark(); mark = buf.length; i++ }
                c == '《' -> {
                    val close = text.indexOf('》', i + 1)
                    val baseStart = if (mark >= 0) mark else kanjiRunStart(buf)
                    if (close < 0 || baseStart >= buf.length || close == i + 1) {
                        restoreMark(); buf.append(c); i++
                    } else {
                        val base = buf.substring(baseStart)
                        out.plain(buf.substring(0, baseStart))
                        out.segment(RubySegment(base, text.substring(i + 1, close)))
                        buf.setLength(0); mark = -1
                        i = close + 1
                    }
                }
                c == '\n' -> { restoreMark(); buf.append(c); i++ }
                else -> { buf.append(c); i++ }
            }
        }
        restoreMark()
        out.plain(buf.toString())
        return out.build()
    }

    private fun kanjiRunStart(buf: CharSequence): Int {
        var i = buf.length
        while (i > 0) {
            val cp = Character.codePointBefore(buf, i)
            if (!isAozoraKanji(cp)) break
            i -= Character.charCount(cp)
        }
        return i
    }

    private fun isAozoraKanji(cp: Int): Boolean =
        Scripts.isHan(cp) || cp == 0x3006 || cp == 0x30F6 || cp == 0x30F5
}

/** Collects segments, merging adjacent plain text. */
internal class SegmentSink {
    private val segments = ArrayList<RubySegment>()
    private val plain = StringBuilder()

    fun plain(text: String) { plain.append(text) }

    fun lastChar(): Char? = if (plain.isNotEmpty()) plain.last() else segments.lastOrNull()?.base?.lastOrNull()

    fun trimTrailingSpace(alsoNewlines: Boolean = false) {
        while (plain.isNotEmpty() && (plain.last() == ' ' || (alsoNewlines && plain.last() == '\n'))) {
            plain.setLength(plain.length - 1)
        }
    }

    fun segment(segment: RubySegment) {
        if (!segment.hasRuby) { plain.append(segment.base); return }
        flush(); segments += segment
    }

    private fun flush() {
        if (plain.isNotEmpty()) { segments += RubySegment(plain.toString()); plain.setLength(0) }
    }

    fun build(): RubyString { flush(); return RubyString(segments) }
}
