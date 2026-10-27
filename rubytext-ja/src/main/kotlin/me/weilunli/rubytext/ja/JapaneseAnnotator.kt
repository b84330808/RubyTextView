package me.weilunli.rubytext.ja

import com.atilika.kuromoji.ipadic.Tokenizer
import me.weilunli.rubytext.RubySegment
import me.weilunli.rubytext.RubyString

/**
 * Adds furigana to Japanese text, offline, with the kuromoji morphological analyzer and
 * mecab-ipadic. Readings go on the kanji only: okurigana and kana inside a word stay
 * bare (取り扱い → 取(と)り扱(あつか)い).
 *
 * Building the tokenizer loads the dictionary (a second or so, tens of MB): create one
 * instance and reuse it, off the main thread. Readings are dictionary readings, so names
 * and rare words can be wrong; review text where accuracy matters.
 *
 * ```
 * val ja = JapaneseAnnotator()
 * RubyText(ja.annotate("東京へ行きます"))
 * ```
 */
public class JapaneseAnnotator(private val tokenizer: Tokenizer = Tokenizer()) {

    /** Annotates every word that contains kanji. Other text stays plain. */
    public fun annotate(text: String): RubyString {
        val segments = ArrayList<RubySegment>()
        for (token in tokenizer.tokenize(text)) {
            val surface = token.surface
            val reading = token.reading
            if (!surface.any { isKanji(it.code) } || reading.isNullOrEmpty() || reading == "*") {
                segments += RubySegment(surface)
                continue
            }
            segments += align(surface, toHiragana(reading))
        }
        return RubyString(segments)
    }

    public companion object {
        /** Katakana to hiragana; other characters unchanged. */
        public fun toHiragana(text: String): String = buildString(text.length) {
            for (c in text) append(if (c in 'ァ'..'ヶ' || c == 'ヽ' || c == 'ヾ') c - 0x60 else c)
        }

        internal fun isKanji(cp: Int): Boolean =
            cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0xF900..0xFAFF ||
                cp == 0x3005 || cp == 0x3006 || cp == 0x3007 || cp == 0x30F6 || cp == 0x30F5 ||
                Character.isSurrogate(cp.toChar())

        /**
         * Splits [surface] into kanji runs and kana, and gives each kanji run the part of
         * [reading] (hiragana) between the kana around it. Falls back to one reading for
         * the whole word when the kana don't line up.
         */
        public fun align(surface: String, reading: String): List<RubySegment> {
            val runs = ArrayList<Pair<String, Boolean>>() // (text, isKanji)
            for (c in surface) {
                val k = isKanji(c.code)
                if (runs.isNotEmpty() && runs.last().second == k) {
                    runs[runs.size - 1] = runs.last().first + c to k
                } else {
                    runs += c.toString() to k
                }
            }
            if (runs.size == 1) return listOf(RubySegment(surface, reading))
            val pattern = buildString {
                append('^')
                for ((t, k) in runs) append(if (k) "(.+?)" else Regex.escape(toHiragana(t)))
                append('$')
            }
            val match = Regex(pattern).matchEntire(reading)
                ?: return listOf(RubySegment(surface, reading))
            val out = ArrayList<RubySegment>()
            var group = 1
            for ((t, k) in runs) {
                out += if (k) RubySegment(t, match.groupValues[group++]) else RubySegment(t)
            }
            return out
        }
    }
}
