package me.weilunli.rubytext

/** Hanyu Pinyin helpers. */
public object Pinyin {
    private val marks = mapOf(
        'a' to "āáǎà", 'e' to "ēéěè", 'i' to "īíǐì", 'o' to "ōóǒò", 'u' to "ūúǔù", 'ü' to "ǖǘǚǜ",
        'A' to "ĀÁǍÀ", 'E' to "ĒÉĚÈ", 'I' to "ĪÍǏÌ", 'O' to "ŌÓǑÒ", 'U' to "ŪÚǓÙ", 'Ü' to "ǕǗǙǛ",
    )
    private val syllable = Regex("([A-Za-zÜüÊê:]+)([0-5])")

    /**
     * Converts tone numbers to tone marks: `ni3 hao3` → `nǐ hǎo`, `lv4` → `lǜ`,
     * `nu:3` → `nǚ`, `ma5`/`ma0` → `ma`. Text without tone numbers is returned unchanged.
     * The mark goes on `a` or `e` if present, on the `o` of `ou`, otherwise on the last vowel.
     */
    public fun fromNumbered(text: String): String = syllable.replace(text) { m ->
        val letters = m.groupValues[1].replace("u:", "ü").replace("U:", "Ü")
            .let { if (it.any { c -> c == 'v' || c == 'V' } && it.length > 1) it.replace('v', 'ü').replace('V', 'Ü') else it }
        val tone = m.groupValues[2][0] - '0'
        if (tone == 0 || tone == 5) letters else addMark(letters, tone) ?: m.value
    }

    private fun addMark(s: String, tone: Int): String? {
        val lower = s.lowercase()
        val index = when {
            'a' in lower -> lower.indexOf('a')
            'e' in lower -> lower.indexOf('e')
            "ou" in lower -> lower.indexOf("ou")
            else -> lower.indexOfLast { it in "iouü" }
        }
        if (index < 0) return null
        val c = s[index]
        val replacement = marks[c]?.get(tone - 1) ?: return null
        return s.substring(0, index) + replacement + s.substring(index + 1)
    }
}
