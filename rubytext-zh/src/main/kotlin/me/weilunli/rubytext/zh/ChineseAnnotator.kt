package me.weilunli.rubytext.zh

import me.weilunli.rubytext.RubySegment
import me.weilunli.rubytext.RubyString
import java.io.InputStream
import java.util.zip.GZIPInputStream

/** Which reading standard to use. */
public enum class ChineseStandard {
    /** Taiwan (國語): McBopomofo-based readings, e.g. 垃圾 ㄌㄜˋ ㄙㄜˋ. */
    Taiwan,

    /** Mainland (普通话): CC-CEDICT-based readings, e.g. 垃圾 ㄌㄚ ㄐㄧ. */
    Mainland,
}

/** How readings are written. */
public enum class ChineseNotation { Zhuyin, Pinyin }

/**
 * Adds Zhuyin or Pinyin to Chinese text, offline. Polyphonic characters get the reading
 * of the word they are in (銀行 ㄧㄣˊ ㄏㄤˊ, 行走 ㄒㄧㄥˊ ㄗㄡˇ): each run of Han characters
 * is split into dictionary words, fewest words first, then the most frequent.
 *
 * The reading tables are CC BY-SA 4.0 (see this artifact's NOTICE). They load lazily on
 * first use, which takes a moment and some memory: create one instance and reuse it,
 * off the main thread.
 *
 * ```
 * val zh = ChineseAnnotator()
 * RubyText(zh.annotate("我去銀行領錢"))                      // Zhuyin on the right
 * RubyText(zh.annotate("我去银行取钱", notation = ChineseNotation.Pinyin))
 * ```
 */
public class ChineseAnnotator {
    private val tables = HashMap<ChineseStandard, ReadingTable>()
    private val variants: Variants by lazy { Variants.parse(resourceText("zh-variants.json.gz")) }

    private fun table(standard: ChineseStandard): ReadingTable = synchronized(tables) {
        tables.getOrPut(standard) {
            ReadingTable(resourceText(if (standard == ChineseStandard.Taiwan) "zh-tw.txt.gz" else "zh-cn.txt.gz"))
        }
    }

    /** Guesses the standard from Simplified/Traditional-only characters; null if unsure. */
    public fun detectStandard(text: String): ChineseStandard? = variants.detect(text)

    /**
     * Annotates [text]. Each word becomes one segment with a reading per character, so
     * `RubyGrouping.Word` keeps words together. Text other than Han characters stays plain.
     *
     * @param standard reading standard; null detects it from the text, falling back to [fallback].
     *   Simplified text read with [ChineseStandard.Taiwan] is looked up via its Traditional form.
     */
    public fun annotate(
        text: String,
        standard: ChineseStandard? = null,
        notation: ChineseNotation = ChineseNotation.Zhuyin,
        fallback: ChineseStandard = ChineseStandard.Taiwan,
    ): RubyString {
        val resolved = standard ?: detectStandard(text) ?: fallback
        val lookup: ((String) -> String)? = if (resolved == ChineseStandard.Taiwan) variants::toTraditional else null
        val segments = ArrayList<RubySegment>()
        for (piece in table(resolved).annotate(text, lookup)) {
            val readings = piece.syllables
            if (readings == null || readings.all { it == null }) {
                segments += RubySegment(piece.text)
            } else {
                segments += RubySegment(
                    piece.text,
                    readings.map { s ->
                        when {
                            s == null -> ""
                            notation == ChineseNotation.Pinyin -> ZhuyinPinyin.toPinyin(s)
                            else -> ZhuyinPinyin.display(s)
                        }
                    },
                )
            }
        }
        return RubyString(segments)
    }

    private fun resourceText(name: String): String {
        val stream: InputStream = ChineseAnnotator::class.java.getResourceAsStream(name)
            ?: error("Missing resource $name")
        return GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}

/** A word, or plain text when [syllables] is null. [syllables] has one entry per code point. */
internal class AnnotatedPiece(val text: String, val syllables: List<String?>?)

internal class ReadingTable(text: String) {
    private class Word(val freq: Int, val syllables: List<String>?)

    private val words = HashMap<String, Word>(1 shl 19)
    private val chars = HashMap<String, List<String>>(1 shl 16)

    init {
        for (line in text.lineSequence()) {
            if (line.isEmpty() || line[0] == '#') continue
            val parts = line.split('\t')
            val w = parts[0]
            val freq = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val readings = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }?.split(' ')
            if (w.codePointCount(0, w.length) == 1) {
                chars[w] = readings ?: emptyList()
                words[w] = Word(freq, null)
            } else {
                words[w] = Word(freq, readings)
            }
        }
    }

    /** Tie-breaker between segmentations with the same number of words. */
    private fun score(freq: Int, len: Int): Double = freq / 2.0 - (if (len == 1) 8 else 0)

    private fun defaultReading(ch: String): String? = chars[ch]?.firstOrNull()

    /** Splits a run of Han characters into (word, syllables). */
    fun segment(run: List<String>): List<Pair<List<String>, List<String?>>> {
        val n = run.size
        val count = IntArray(n + 1) { Int.MAX_VALUE }
        val best = DoubleArray(n + 1) { Double.NEGATIVE_INFINITY }
        val back = IntArray(n + 1)
        count[0] = 0
        best[0] = 0.0
        for (i in 0 until n) {
            if (count[i] == Int.MAX_VALUE) continue
            val w = StringBuilder()
            var j = i
            while (j < n && j - i < MAX_WORD) {
                w.append(run[j])
                val entry = words[w.toString()]
                val s = when {
                    entry != null -> score(entry.freq, j - i + 1)
                    j == i -> score(0, 1) - 10
                    else -> { j++; continue }
                }
                val c = count[i] + 1
                val b = best[i] + s
                if (c < count[j + 1] || (c == count[j + 1] && b > best[j + 1])) {
                    count[j + 1] = c
                    best[j + 1] = b
                    back[j + 1] = i
                }
                j++
            }
        }
        val out = ArrayList<Pair<List<String>, List<String?>>>()
        var end = n
        while (end > 0) {
            val start = back[end]
            val cs = run.subList(start, end)
            val syllables: List<String?> = words[cs.joinToString("")]?.syllables?.takeIf { it.size == cs.size }
                ?: cs.map { defaultReading(it) }
            out += cs to syllables
            end = start
        }
        out.reverse()
        return out
    }

    /**
     * Annotates [text]. [lookup] maps the text before lookup (e.g. Simplified to Traditional)
     * without changing its length; the original characters are kept in the output.
     */
    fun annotate(text: String, lookup: ((String) -> String)?): List<AnnotatedPiece> {
        val out = ArrayList<AnnotatedPiece>()
        var src = lookup?.invoke(text) ?: text
        if (src.length != text.length) src = text
        var last = 0
        for ((start, end) in hanRuns(src)) {
            if (start > last) out += AnnotatedPiece(text.substring(last, start), null)
            val original = codePoints(text.substring(start, end))
            var k = 0
            for ((word, syllables) in segment(codePoints(src.substring(start, end)))) {
                out += AnnotatedPiece(original.subList(k, k + word.size).joinToString(""), syllables)
                k += word.size
            }
            last = end
        }
        if (last < text.length) out += AnnotatedPiece(text.substring(last), null)
        return out
    }

    companion object {
        const val MAX_WORD = 8

        /** Han characters (and 〇) by range: `Character.UnicodeScript` needs Android API 24. */
        fun isHan(cp: Int): Boolean =
            cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x323AF ||
                cp in 0xF900..0xFAFF || cp in 0x2F800..0x2FA1F || cp in 0x2E80..0x2FDF ||
                cp == 0x3005 || cp == 0x3007 || cp in 0x3021..0x3029 || cp in 0x3038..0x303B

        /** [start, end) ranges of Han runs in [s]. */
        fun hanRuns(s: String): List<Pair<Int, Int>> {
            val out = ArrayList<Pair<Int, Int>>()
            var i = 0
            var runStart = -1
            while (i < s.length) {
                val cp = s.codePointAt(i)
                if (isHan(cp)) {
                    if (runStart < 0) runStart = i
                } else if (runStart >= 0) {
                    out += runStart to i
                    runStart = -1
                }
                i += Character.charCount(cp)
            }
            if (runStart >= 0) out += runStart to s.length
            return out
        }

        fun codePoints(s: String): List<String> {
            val out = ArrayList<String>(s.length)
            var i = 0
            while (i < s.length) {
                val n = Character.charCount(s.codePointAt(i))
                out += s.substring(i, i + n)
                i += n
            }
            return out
        }
    }
}

/** Simplified/Traditional character sets from Unihan. */
internal class Variants(
    private val simplifiedOnly: Set<Int>,
    private val traditionalOnly: Set<Int>,
    private val s2t: Map<Int, Int>,
) {
    fun detect(text: String): ChineseStandard? {
        var s = 0
        var t = 0
        for (cp in codePointArray(text)) {
            if (cp in simplifiedOnly) s++ else if (cp in traditionalOnly) t++
        }
        return when {
            s > t -> ChineseStandard.Mainland
            t > s -> ChineseStandard.Taiwan
            else -> null
        }
    }

    fun toTraditional(text: String): String {
        val sb = StringBuilder(text.length)
        for (cp in codePointArray(text)) sb.appendCodePoint(s2t[cp] ?: cp)
        return sb.toString()
    }

    companion object {
        /** Reads `{"simplifiedOnly": "...", "traditionalOnly": "...", "s2t": "..."}`. */
        fun parse(json: String): Variants {
            val fields = parseStringObject(json)
            val simp = codePointArray(fields["simplifiedOnly"].orEmpty()).toHashSet()
            val trad = codePointArray(fields["traditionalOnly"].orEmpty()).toHashSet()
            val pairs = codePointArray(fields["s2t"].orEmpty())
            val map = HashMap<Int, Int>(pairs.size)
            var i = 0
            while (i + 1 < pairs.size) { map[pairs[i]] = pairs[i + 1]; i += 2 }
            return Variants(simp, trad, map)
        }

        /** A JSON object whose values are all strings. */
        internal fun parseStringObject(json: String): Map<String, String> {
            val out = HashMap<String, String>()
            var i = json.indexOf('{') + 1
            fun skipSpace() { while (i < json.length && json[i].isWhitespace()) i++ }
            fun readString(): String {
                check(json[i] == '"') { "Expected a string at $i" }
                i++
                val sb = StringBuilder()
                while (json[i] != '"') {
                    val c = json[i++]
                    if (c != '\\') { sb.append(c); continue }
                    when (val e = json[i++]) {
                        'u' -> { sb.append(json.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        else -> sb.append(e)
                    }
                }
                i++
                return sb.toString()
            }
            while (true) {
                skipSpace()
                if (i >= json.length || json[i] == '}') break
                val key = readString()
                skipSpace(); check(json[i] == ':'); i++; skipSpace()
                out[key] = readString()
                skipSpace()
                if (i < json.length && json[i] == ',') i++
            }
            return out
        }
    }
}

internal fun codePointArray(s: String): IntArray {
    val out = IntArray(s.codePointCount(0, s.length))
    var i = 0
    var k = 0
    while (i < s.length) {
        val cp = s.codePointAt(i)
        out[k++] = cp
        i += Character.charCount(cp)
    }
    return out
}
