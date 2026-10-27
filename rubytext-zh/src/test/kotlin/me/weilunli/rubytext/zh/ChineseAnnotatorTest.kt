package me.weilunli.rubytext.zh

import me.weilunli.rubytext.RubyString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Cases ported from Ruby Everywhere's test/unit/zh.test.js and zhuyin.test.js. */
class ChineseAnnotatorTest {
    companion object {
        val zh = ChineseAnnotator()
    }

    /** `銀(ㄧㄣˊ)行(ㄏㄤˊ)`: each character followed by its reading. */
    private fun show(s: RubyString): String = buildString {
        for (seg in s.segments) {
            if (!seg.hasRuby) { append(seg.base); continue }
            seg.characters.forEachIndexed { i, ch ->
                append(ch)
                val r = seg.readings[i]
                if (r.isNotEmpty()) append('(').append(r).append(')')
            }
        }
    }

    private fun read(text: String, standard: ChineseStandard? = ChineseStandard.Taiwan) = show(zh.annotate(text, standard))

    @Test
    fun polyphonesTakeTheReadingOfTheirWord() {
        val cases = listOf(
            "銀行" to "銀(ㄧㄣˊ)行(ㄏㄤˊ)",
            "行走" to "行(ㄒㄧㄥˊ)走(ㄗㄡˇ)",
            "長大" to "長(ㄓㄤˇ)大(ㄉㄚˋ)",
            "長江" to "長(ㄔㄤˊ)江(ㄐㄧㄤ)",
            "音樂" to "音(ㄧㄣ)樂(ㄩㄝˋ)",
            "快樂" to "快(ㄎㄨㄞˋ)樂(ㄌㄜˋ)",
            "重要" to "重(ㄓㄨㄥˋ)要(ㄧㄠˋ)",
            "重新" to "重(ㄔㄨㄥˊ)新(ㄒㄧㄣ)",
            "睡覺" to "睡(ㄕㄨㄟˋ)覺(ㄐㄧㄠˋ)",
            "覺得" to "覺(ㄐㄩㄝˊ)得(˙ㄉㄜ)",
            "還給" to "還(ㄏㄨㄢˊ)給(ㄍㄟˇ)",
            "便宜" to "便(ㄆㄧㄢˊ)宜(ㄧˊ)",
            "方便" to "方(ㄈㄤ)便(ㄅㄧㄢˋ)",
            "看著" to "看(ㄎㄢˋ)著(˙ㄓㄜ)",
            "垃圾" to "垃(ㄌㄜˋ)圾(ㄙㄜˋ)",
        )
        for ((text, want) in cases) assertEquals(want, read(text), text)
    }

    @Test
    fun sentences() {
        assertEquals(
            "我(ㄨㄛˇ)去(ㄑㄩˋ)銀(ㄧㄣˊ)行(ㄏㄤˊ)領(ㄌㄧㄥˇ)錢(ㄑㄧㄢˊ)，然(ㄖㄢˊ)後(ㄏㄡˋ)在(ㄗㄞˋ)路(ㄌㄨˋ)上(ㄕㄤˋ)行(ㄒㄧㄥˊ)走(ㄗㄡˇ)。",
            read("我去銀行領錢，然後在路上行走。"),
        )
        assertEquals("他(ㄊㄚ)想(ㄒㄧㄤˇ)當(ㄉㄤ)行(ㄏㄤˊ)長(ㄓㄤˇ)。", read("他想當行長。"))
    }

    @Test
    fun wordsBecomeSegments() {
        val s = zh.annotate("我去銀行", ChineseStandard.Taiwan)
        assertTrue(s.segments.any { it.base == "銀行" && it.readings == listOf("ㄧㄣˊ", "ㄏㄤˊ") })
    }

    @Test
    fun mainlandAndDetection() {
        assertEquals("银(ㄧㄣˊ)行(ㄏㄤˊ)", read("银行", null))
        assertEquals("垃(ㄌㄚ)圾(ㄐㄧ)", read("垃圾", ChineseStandard.Mainland))
        assertEquals("音(ㄧㄣ)乐(ㄩㄝˋ)", read("音乐", null))
        assertEquals(ChineseStandard.Mainland, zh.detectStandard("我们去银行"))
        assertEquals(ChineseStandard.Taiwan, zh.detectStandard("我們去銀行"))
        // Simplified text read with Taiwan readings.
        assertEquals("银(ㄧㄣˊ)行(ㄏㄤˊ)", read("银行", ChineseStandard.Taiwan))
    }

    @Test
    fun otherTextStaysPlain() {
        assertEquals("Hello 世(ㄕˋ)界(ㄐㄧㄝˋ) 123！", read("Hello 世界 123！"))
        assertEquals("abc", read("abc"))
    }

    @Test
    fun pinyin() {
        val s = zh.annotate("我去銀行", ChineseStandard.Taiwan, ChineseNotation.Pinyin)
        assertEquals("我(wǒ)去(qù)銀(yín)行(háng)", show(s))
    }

    @Test
    fun zhuyinToPinyin() {
        val cases = mapOf(
            "ㄏㄤˊ" to "háng", "ㄒㄧㄥˊ" to "xíng", "ㄓㄜ˙" to "zhe", "ㄌㄩˋ" to "lǜ", "ㄌㄩㄝˋ" to "lüè", "ㄐㄩ" to "jū",
            "ㄑㄩㄥˊ" to "qióng", "ㄓ" to "zhī", "ㄙˋ" to "sì", "ㄦˊ" to "ér", "ㄨㄛˇ" to "wǒ", "ㄍㄨㄟˋ" to "guì",
            "ㄌㄧㄡˊ" to "liú", "ㄅㄛ" to "bō", "ㄈㄥ" to "fēng", "ㄩㄝˋ" to "yuè", "ㄒㄩㄝˊ" to "xué", "ㄧ" to "yī",
            "ㄨㄥ" to "wēng", "ㄉㄨㄥˋ" to "dòng", "ㄋㄩˇ" to "nǚ", "ㄧㄡˇ" to "yǒu", "ㄏㄨㄚ" to "huā",
        )
        for ((z, p) in cases) assertEquals(p, ZhuyinPinyin.toPinyin(z), z)
        assertEquals("hang2", ZhuyinPinyin.toNumbered("ㄏㄤˊ"))
        assertEquals("˙ㄉㄜ", ZhuyinPinyin.display("ㄉㄜ˙"))
        assertEquals("ㄏㄤˊ", ZhuyinPinyin.display("ㄏㄤˊ"))
    }

    @Test
    fun extensionBCharacters() {
        // A supplementary-plane character must not break the alignment of readings.
        val s = zh.annotate("𠮟銀行", ChineseStandard.Taiwan)
        assertEquals("銀(ㄧㄣˊ)行(ㄏㄤˊ)", show(s).substringAfter("𠮟").replace(Regex("^\\([^)]*\\)"), ""))
    }
}
