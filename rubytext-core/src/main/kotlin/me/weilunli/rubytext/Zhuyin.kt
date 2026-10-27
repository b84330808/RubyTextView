package me.weilunli.rubytext

/** Mandarin tones as written in Zhuyin. */
public enum class ZhuyinTone(public val mark: Char?) {
    /** 陰平. Unmarked in Taiwan; `ˉ` is accepted on input. */
    First(null),
    /** 陽平 `ˊ`. */
    Second('ˊ'),
    /** 上聲 `ˇ`. */
    Third('ˇ'),
    /** 去聲 `ˋ`. */
    Fourth('ˋ'),
    /** 輕聲 `˙`, written before the symbols. */
    Neutral('˙'),
}

/** One Zhuyin syllable: up to three symbols and a tone, e.g. `ㄏㄠˇ`. */
public data class ZhuyinSyllable(val symbols: String, val tone: ZhuyinTone) {
    /** Written form: `˙ㄇㄚ` for the neutral tone, `ㄏㄠˇ` otherwise, no mark for the first tone. */
    override fun toString(): String = when (tone) {
        ZhuyinTone.Neutral -> "˙$symbols"
        ZhuyinTone.First -> symbols
        else -> symbols + tone.mark
    }
}

/** Zhuyin (Bopomofo, 注音符號) helpers. */
public object Zhuyin {
    /** Bopomofo letters, including the extended block used for Taiwanese and Hakka. */
    public fun isSymbol(cp: Int): Boolean = cp in 0x3105..0x312F || cp in 0x31A0..0x31BF

    /** Tone marks: `ˉ ˊ ˇ ˋ ˙`, plus `·` (often typed for the neutral tone) and the dialect marks `˪ ˫`. */
    public fun isToneMark(c: Char): Boolean =
        c == 'ˉ' || c == 'ˊ' || c == 'ˇ' || c == 'ˋ' || c == '˙' || c == '·' || c == '˪' || c == '˫'

    /** True when [text] has at least one Bopomofo letter and nothing but letters, tone marks and spaces. */
    public fun isZhuyin(text: String): Boolean {
        var symbols = 0
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            when {
                isSymbol(cp) -> symbols++
                cp < 0x10000 && isToneMark(cp.toChar()) -> Unit
                Character.isWhitespace(cp) -> Unit
                else -> return false
            }
            i += Character.charCount(cp)
        }
        return symbols > 0
    }

    /**
     * Parses one syllable such as `ㄏㄠˇ`, `˙ㄇㄚ`, `ㄇㄚ˙` or `ㄓ`. Returns null if [text]
     * is not exactly one syllable of Bopomofo letters with at most one tone mark.
     */
    public fun parseSyllable(text: String): ZhuyinSyllable? {
        val t = text.trim()
        if (t.isEmpty()) return null
        var tone = ZhuyinTone.First
        var toneSeen = false
        var toneAfterSymbols = false
        val symbols = StringBuilder()
        var i = 0
        while (i < t.length) {
            val cp = t.codePointAt(i)
            if (isSymbol(cp)) {
                // Only a neutral dot may come before the symbols; any mark after them ends the syllable.
                if (toneAfterSymbols) return null
                symbols.appendCodePoint(cp)
            } else if (cp < 0x10000 && isToneMark(cp.toChar())) {
                if (toneSeen) return null
                toneSeen = true
                tone = toneOf(cp.toChar()) ?: return null
                if (symbols.isEmpty()) {
                    if (tone != ZhuyinTone.Neutral) return null
                } else {
                    toneAfterSymbols = true
                }
            } else {
                return null
            }
            i += Character.charCount(cp)
        }
        if (symbols.isEmpty()) return null
        return ZhuyinSyllable(symbols.toString(), tone)
    }

    private fun toneOf(c: Char): ZhuyinTone? = when (c) {
        'ˉ' -> ZhuyinTone.First
        'ˊ' -> ZhuyinTone.Second
        'ˇ' -> ZhuyinTone.Third
        'ˋ' -> ZhuyinTone.Fourth
        '˙', '·' -> ZhuyinTone.Neutral
        else -> null // dialect marks: kept as text by callers
    }

    private fun isInitial(cp: Int) = cp in 0x3105..0x3119 || cp in 0x312A..0x312C || cp in 0x31A0..0x31A3
    private fun isMedial(cp: Int) = cp in 0x3127..0x3129
    private fun isFinal(cp: Int) = cp in 0x311A..0x3126 || cp in 0x312D..0x312F || cp in 0x31A4..0x31BF

    /**
     * Splits run-together Zhuyin into syllables: `ㄋㄧˇㄏㄠˇ` → `ㄋㄧˇ`, `ㄏㄠˇ`.
     * Whitespace always separates syllables. Without whitespace the split is greedy
     * (initial, medial, final, tone), so `ㄓㄨㄥㄨㄣˊ` gives `ㄓㄨㄥ`, `ㄨㄣˊ`. First-tone
     * syllables written together can be ambiguous (`ㄒㄧㄢ` vs `ㄒㄧ ㄢ`); use spaces then.
     */
    public fun splitSyllables(text: String): List<String> {
        val out = ArrayList<String>()
        for (word in text.trim().split(Regex("\\s+"))) {
            if (word.isEmpty()) continue
            var i = 0
            while (i < word.length) {
                val start = i
                // leading neutral dot
                if (word[i] == '˙' || word[i] == '·') i++
                var stage = 0 // 0 nothing, 1 initial, 2 medial, 3 final
                while (i < word.length) {
                    val cp = word.codePointAt(i)
                    val next = when {
                        isInitial(cp) -> if (stage == 0) 1 else -1
                        isMedial(cp) -> if (stage < 2) 2 else -1
                        isFinal(cp) -> if (stage < 3) 3 else -1
                        else -> -1
                    }
                    if (next < 0) break
                    stage = next
                    i += Character.charCount(cp)
                }
                // trailing tone mark (a neutral dot here closes the syllable too,
                // unless the syllable already started with one)
                if (i < word.length && isToneMark(word[i]) && stage > 0) {
                    val leadingDot = word[start] == '˙' || word[start] == '·'
                    if (!(leadingDot && (word[i] == '˙' || word[i] == '·'))) i++
                }
                if (i == start) i++ // unknown character: emit it on its own
                out += word.substring(start, i)
            }
        }
        return out
    }
}
