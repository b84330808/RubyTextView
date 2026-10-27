package me.weilunli.rubytext.zh

import java.text.Normalizer

/**
 * Zhuyin to Hanyu Pinyin conversion, for one syllable at a time.
 * Syllables are written with the tone mark last (`ㄏㄤˊ`, `ㄓㄜ˙`); the first tone has no mark.
 */
public object ZhuyinPinyin {
    private val initials = mapOf(
        "ㄅ" to "b", "ㄆ" to "p", "ㄇ" to "m", "ㄈ" to "f", "ㄉ" to "d", "ㄊ" to "t", "ㄋ" to "n", "ㄌ" to "l",
        "ㄍ" to "g", "ㄎ" to "k", "ㄏ" to "h", "ㄐ" to "j", "ㄑ" to "q", "ㄒ" to "x", "ㄓ" to "zh", "ㄔ" to "ch",
        "ㄕ" to "sh", "ㄖ" to "r", "ㄗ" to "z", "ㄘ" to "c", "ㄙ" to "s",
    )

    // Finals after an initial.
    private val finals = mapOf(
        "ㄚ" to "a", "ㄛ" to "o", "ㄜ" to "e", "ㄝ" to "ê", "ㄞ" to "ai", "ㄟ" to "ei", "ㄠ" to "ao", "ㄡ" to "ou",
        "ㄢ" to "an", "ㄣ" to "en", "ㄤ" to "ang", "ㄥ" to "eng", "ㄦ" to "er",
        "ㄧ" to "i", "ㄧㄚ" to "ia", "ㄧㄛ" to "io", "ㄧㄝ" to "ie", "ㄧㄞ" to "iai", "ㄧㄠ" to "iao", "ㄧㄡ" to "iu",
        "ㄧㄢ" to "ian", "ㄧㄣ" to "in", "ㄧㄤ" to "iang", "ㄧㄥ" to "ing",
        "ㄨ" to "u", "ㄨㄚ" to "ua", "ㄨㄛ" to "uo", "ㄨㄞ" to "uai", "ㄨㄟ" to "ui", "ㄨㄢ" to "uan", "ㄨㄣ" to "un",
        "ㄨㄤ" to "uang", "ㄨㄥ" to "ong",
        "ㄩ" to "ü", "ㄩㄝ" to "üe", "ㄩㄢ" to "üan", "ㄩㄣ" to "ün", "ㄩㄥ" to "iong",
    )

    // Syllables without an initial.
    private val zeroInitial = mapOf(
        "ㄚ" to "a", "ㄛ" to "o", "ㄜ" to "e", "ㄝ" to "ê", "ㄞ" to "ai", "ㄟ" to "ei", "ㄠ" to "ao", "ㄡ" to "ou",
        "ㄢ" to "an", "ㄣ" to "en", "ㄤ" to "ang", "ㄥ" to "eng", "ㄦ" to "er",
        "ㄧ" to "yi", "ㄧㄚ" to "ya", "ㄧㄛ" to "yo", "ㄧㄝ" to "ye", "ㄧㄞ" to "yai", "ㄧㄠ" to "yao", "ㄧㄡ" to "you",
        "ㄧㄢ" to "yan", "ㄧㄣ" to "yin", "ㄧㄤ" to "yang", "ㄧㄥ" to "ying",
        "ㄨ" to "wu", "ㄨㄚ" to "wa", "ㄨㄛ" to "wo", "ㄨㄞ" to "wai", "ㄨㄟ" to "wei", "ㄨㄢ" to "wan", "ㄨㄣ" to "wen",
        "ㄨㄤ" to "wang", "ㄨㄥ" to "weng",
        "ㄩ" to "yu", "ㄩㄝ" to "yue", "ㄩㄢ" to "yuan", "ㄩㄣ" to "yun", "ㄩㄥ" to "yong",
        // Syllabic nasals in interjections.
        "ㄇ" to "m", "ㄋ" to "n", "ㄫ" to "ng", "ㄏㄇ" to "hm", "ㄏㄫ" to "hng",
    )

    private val combining = arrayOf("", "̄", "́", "̌", "̀")

    /** Splits a syllable into symbols and tone 1..5 (5 = neutral). */
    internal fun splitTone(syllable: String): Pair<String, Int> {
        if (syllable.isEmpty()) return syllable to 1
        val tone = when (syllable.last()) {
            'ˊ' -> 2; 'ˇ' -> 3; 'ˋ' -> 4; '˙' -> 5; 'ˉ' -> 1
            else -> 0
        }
        if (tone != 0) return syllable.dropLast(1) to tone
        if (syllable.first() == '˙') return syllable.drop(1) to 5
        return syllable to 1
    }

    /** `ㄏㄤˊ` → (`hang`, 2), or null if [syllable] is not a Mandarin syllable. */
    public fun toPinyinParts(syllable: String): Pair<String, Int>? {
        val (body, tone) = splitTone(syllable)
        if (body.isEmpty()) return null
        zeroInitial[body]?.let { return it to tone }
        val initial = initials[body.substring(0, 1)] ?: return null
        val rest = body.substring(1)
        val final = if (rest.isEmpty()) {
            if (initial !in setOf("zh", "ch", "sh", "r", "z", "c", "s")) return null
            "i"
        } else {
            val f = finals[rest] ?: return null
            if (initial == "j" || initial == "q" || initial == "x") f.replace('ü', 'u') else f
        }
        return initial + final to tone
    }

    /** Puts the tone mark on the right vowel: (`hang`, 2) → `háng`. */
    public fun addToneMark(base: String, tone: Int): String {
        if (tone < 1 || tone > 4) return base
        var index = -1
        for (v in listOf('a', 'e', 'ê')) {
            val i = base.indexOf(v)
            if (i >= 0) { index = i; break }
        }
        if (index < 0 && "ou" in base) index = base.indexOf('o')
        if (index < 0) index = base.indexOfLast { it in "iouü" }
        if (index < 0) index = base.indexOfFirst { it == 'm' || it == 'n' } // syllabic nasals
        if (index < 0) return base
        return Normalizer.normalize(base.substring(0, index + 1) + combining[tone] + base.substring(index + 1), Normalizer.Form.NFC)
    }

    /** `ㄏㄤˊ` → `háng`. Returns [syllable] unchanged if it is not a Mandarin syllable. */
    public fun toPinyin(syllable: String): String {
        val (base, tone) = toPinyinParts(syllable) ?: return syllable
        return addToneMark(base, tone)
    }

    /** `ㄏㄤˊ` → `hang2`, or null. */
    public fun toNumbered(syllable: String): String? = toPinyinParts(syllable)?.let { (base, tone) -> base + tone }

    /** Display form: the neutral-tone dot goes in front (`ㄉㄜ˙` → `˙ㄉㄜ`). */
    public fun display(syllable: String): String {
        val (body, tone) = splitTone(syllable)
        return when (tone) {
            5 -> "˙$body"
            2 -> "${body}ˊ"
            3 -> "${body}ˇ"
            4 -> "${body}ˋ"
            else -> body
        }
    }
}
