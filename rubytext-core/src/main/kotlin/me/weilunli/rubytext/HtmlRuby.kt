package me.weilunli.rubytext

/**
 * Reads HTML ruby markup into a [RubyString]. This is a small tolerant reader for
 * ruby fragments, not a full HTML parser.
 *
 * - `<ruby>漢<rt>かん</rt>字<rt>じ</rt></ruby>` and `<ruby><rb>漢</rb><rb>字</rb><rt>かん</rt><rt>じ</rt></ruby>`
 *   both pair each base with its `<rt>`. Single-character pairs inside one `<ruby>` become one
 *   segment with a reading per character, so they wrap and align as a word.
 * - `<rp>` fallback parentheses and `<rtc>` wrappers are ignored.
 * - Other tags are dropped and their text kept. `<br>` and the end of `<p>`, `<div>`, `<li>`
 *   and headings become line breaks. Whitespace collapses as in HTML.
 * - Character references (`&amp;`, `&#x3042;`, `&nbsp;` ...) are decoded.
 */
public object HtmlRuby {
    private val tagRegex = Regex("<(/?)([a-zA-Z][a-zA-Z0-9]*)\\b[^>]*?(/?)>|<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val blockEnds = setOf("p", "div", "li", "h1", "h2", "h3", "h4", "h5", "h6", "tr", "dt", "dd")

    public fun parse(html: String): RubyString {
        val out = SegmentSink()
        var ruby: RubyCollector? = null
        var skipDepth = 0 // inside <rp>, <script>, <style>
        var inRt = false
        val rtText = StringBuilder()
        var pos = 0
        var lastWasSpace = true

        fun emitText(raw: String) {
            if (skipDepth > 0) return
            val text = decodeEntities(raw)
            when {
                inRt -> rtText.append(text)
                ruby != null -> ruby!!.text(text)
                else -> {
                    val collapsed = collapse(text, lastWasSpace)
                    if (collapsed.isNotEmpty()) {
                        out.plain(collapsed)
                        lastWasSpace = collapsed.last() == ' ' || collapsed.last() == '\n'
                    }
                }
            }
        }

        for (m in tagRegex.findAll(html)) {
            emitText(html.substring(pos, m.range.first))
            pos = m.range.last + 1
            if (m.value.startsWith("<!--")) continue
            val closing = m.groupValues[1] == "/"
            val name = m.groupValues[2].lowercase()
            when (name) {
                "rp", "script", "style", "template" -> skipDepth = if (closing) maxOf(0, skipDepth - 1) else skipDepth + 1
                "ruby" -> if (!closing) {
                    ruby?.finish(out)
                    ruby = RubyCollector()
                } else {
                    if (inRt) { ruby?.rt(rtText.toString()); rtText.setLength(0); inRt = false }
                    ruby?.finish(out); ruby = null
                    lastWasSpace = false
                }
                "rt" -> if (ruby != null) {
                    if (inRt) { ruby!!.rt(rtText.toString()); rtText.setLength(0) }
                    inRt = !closing
                }
                "rb" -> if (ruby != null && !inRt) { if (closing) ruby!!.endRb() else ruby!!.startRb() }
                "br" -> if (ruby == null) { out.trimTrailingSpace(); out.plain("\n"); lastWasSpace = true }
                else -> if (closing && name in blockEnds && ruby == null) {
                    out.trimTrailingSpace()
                    val last = out.lastChar()
                    if (last != null && last != '\n') out.plain("\n")
                    lastWasSpace = true
                }
            }
        }
        emitText(html.substring(pos))
        if (inRt) ruby?.rt(rtText.toString())
        ruby?.finish(out)
        out.trimTrailingSpace(alsoNewlines = true)
        return out.build()
    }

    /** Pairs bases and `<rt>`s inside one `<ruby>` element. */
    private class RubyCollector {
        private val pairs = ArrayList<Pair<String, String?>>() // finished (base, reading)
        private val rbs = ArrayList<String>() // explicit <rb> bases waiting for <rt>
        private val anon = StringBuilder() // base text outside <rb>
        private var rb: StringBuilder? = null
        private var rtIndex = 0 // next <rb> to receive an <rt>

        fun text(t: String) { (rb ?: anon).append(t) }
        fun startRb() { flushAnonAsRb(); rb = StringBuilder() }
        fun endRb() { rb?.let { rbs += it.toString() }; rb = null }

        private fun flushAnonAsRb() {
            val a = anon.toString().trim()
            if (a.isNotEmpty()) rbs += a
            anon.setLength(0)
        }

        fun rt(reading: String) {
            endRb()
            val r = collapse(reading, true).trim()
            if (rbs.isNotEmpty() && rtIndex < rbs.size) {
                // <rb>-style: the n-th <rt> belongs to the n-th <rb>.
                if (anon.isNotBlank()) flushAnonAsRb()
                pairs += rbs[rtIndex].trim() to r
                rtIndex++
                if (rtIndex == rbs.size) { rbs.clear(); rtIndex = 0 }
            } else {
                val base = collapse(anon.toString(), true).trim()
                anon.setLength(0)
                if (base.isNotEmpty()) pairs += base to r
                else if (pairs.isNotEmpty() && pairs.last().second.isNullOrEmpty()) {
                    pairs[pairs.size - 1] = pairs.last().first to r
                }
            }
        }

        fun finish(out: SegmentSink) {
            endRb()
            for (i in rtIndex until rbs.size) pairs += rbs[i].trim() to null
            val tail = collapse(anon.toString(), true).trim()
            if (tail.isNotEmpty()) pairs += tail to null
            // Merge runs of single-character pairs into one per-character segment (one word).
            var i = 0
            while (i < pairs.size) {
                val (base, reading) = pairs[i]
                if (reading.isNullOrEmpty() || Graphemes.count(base) != 1) {
                    out.segment(RubySegment(base, reading)); i++; continue
                }
                var j = i
                while (j < pairs.size && !pairs[j].second.isNullOrEmpty() && Graphemes.count(pairs[j].first) == 1) j++
                if (j - i == 1) out.segment(RubySegment(base, reading))
                else out.segment(RubySegment(pairs.subList(i, j).joinToString("") { it.first }, pairs.subList(i, j).map { it.second!! }))
                i = j
            }
        }
    }

    private fun collapse(text: String, lastWasSpace: Boolean): String {
        val sb = StringBuilder()
        var space = lastWasSpace
        for (c in text) {
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000c') {
                if (!space) sb.append(' ')
                space = true
            } else {
                sb.append(c); space = false
            }
        }
        return sb.toString()
    }

    private val entityRegex = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")
    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "ensp" to " ", "emsp" to " ", "thinsp" to " ", "zwj" to "‍", "zwnj" to "‌",
        "lrm" to "‎", "rlm" to "‏", "middot" to "·", "hellip" to "…", "mdash" to "—", "ndash" to "–",
        "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”", "copy" to "©",
    )

    internal fun decodeEntities(s: String): String {
        if ('&' !in s) return s
        return entityRegex.replace(s) { m ->
            val body = m.groupValues[1]
            when {
                body.startsWith("#x") || body.startsWith("#X") ->
                    body.substring(2).toIntOrNull(16)?.takeIf { Character.isValidCodePoint(it) }
                        ?.let { String(Character.toChars(it)) } ?: m.value
                body.startsWith("#") ->
                    body.substring(1).toIntOrNull()?.takeIf { Character.isValidCodePoint(it) }
                        ?.let { String(Character.toChars(it)) } ?: m.value
                else -> named[body] ?: m.value
            }
        }
    }
}
