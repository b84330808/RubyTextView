package me.weilunli.rubytext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RubyMarkupTest {
    private fun seg(base: String, vararg readings: String) = RubySegment(base, readings.toList())

    @Test
    fun groupReading() {
        val s = RubyString.parse("{東京|とうきょう}へ行く")
        assertEquals(listOf(seg("東京", "とうきょう"), seg("へ行く")), s.segments)
        assertEquals("東京へ行く", s.text)
        assertTrue(!s.segments[0].isMono)
    }

    @Test
    fun perCharacterWithBars() {
        val s = RubyString.parse("{東京|とう|きょう}")
        assertEquals(listOf(seg("東京", "とう", "きょう")), s.segments)
        assertTrue(s.segments[0].isMono)
    }

    @Test
    fun perCharacterWithSpaces() {
        assertEquals(listOf(seg("你好", "ㄋㄧˇ", "ㄏㄠˇ")), RubyString.parse("{你好|ㄋㄧˇ ㄏㄠˇ}").segments)
        assertEquals(listOf(seg("你好", "nǐ", "hǎo")), RubyString.parse("{你好| nǐ  hǎo }").segments)
    }

    @Test
    fun spacedReadingThatDoesNotMatchStaysWhole() {
        assertEquals(
            listOf(seg("北京大学", "Peking University")),
            RubyString.parse("{北京大学|Peking University}").segments,
        )
    }

    @Test
    fun barCountMismatchJoinsReadings() {
        assertEquals(listOf(seg("今日", "きょうx")), RubyString.parse("{今日|き|ょう|x}").segments)
    }

    @Test
    fun escapesAndMalformedGroupsAreLiteral() {
        assertEquals("{a} b|c \\", RubyString.parse("\\{a\\} b|c \\\\").text)
        assertEquals("{no bar}", RubyString.parse("{no bar}").text)
        assertEquals("{open", RubyString.parse("{open").text)
        val nested = RubyString.parse("{x{y|z}")
        assertEquals(listOf(seg("{x"), seg("y", "z")), nested.segments)
        assertEquals(listOf(seg("a|b", "c")), RubyString.parse("{a\\|b|c}").segments)
    }

    @Test
    fun adjacentPlainTextIsMerged() {
        assertEquals(listOf(seg("ab{c")), RubyString.parse("a" + "b" + "{c").segments)
    }

    @Test
    fun markupRoundTrip() {
        val inputs = listOf(
            "{東京|とう|きょう}に{行|い}く",
            "{北京|Běijīng} and {你好|nǐ hǎo}",
            "a \\{literal\\} \\| \\\\ text",
            "{今日|きょう}",
        )
        for (input in inputs) {
            val parsed = RubyString.parse(input)
            assertEquals(parsed, RubyString.parse(parsed.toMarkup()), input)
        }
        // A spaced group reading over several characters survives the round trip.
        val group = RubyString(listOf(RubySegment("北京", "běi jīng")))
        assertEquals("{北京|běi\\ jīng}", group.toMarkup())
        assertEquals(group, RubyString.parse(group.toMarkup()))
    }

    @Test
    fun builder() {
        val s = buildRubyString {
            append("今日は")
            ruby("東京", "とうきょう")
            ruby("你好", listOf("ㄋㄧˇ", "ㄏㄠˇ"))
            append("")
        }
        assertEquals(3, s.segments.size)
        assertEquals("今日は東京你好", s.text)
        assertEquals("今日は東京(とうきょう)你好(ㄋㄧˇ ㄏㄠˇ)", s.toPlainText(includeReadings = true))
    }

    @Test
    fun readingCountMustMatchCharacters() {
        assertFailsWith<IllegalArgumentException> { RubySegment("東京", listOf("と", "う", "きょう")) }
        // Extension B ideograph (two UTF-16 units) is one character.
        assertEquals(1, RubySegment("𠮟", listOf("しっ")).characters.size)
        RubySegment("𠮟る", listOf("しか", ""))
    }

    @Test
    fun legacyV1Format() {
        val s = RubyString.fromLegacy("令|れい 和|わ")
        assertEquals(listOf(seg("令", "れい"), seg("和", "わ")), s.segments)
        val t = RubyString.fromLegacy("全|すべ ての 瞬間|しゅんかん に 価値|かち がある。")
        assertEquals("全ての瞬間に価値がある。", t.text)
        assertEquals(listOf("すべ", "しゅんかん", "かち"), t.segments.filter { it.hasRuby }.map { it.reading })
        assertEquals(listOf(seg("天"), seg("気", "qì")), RubyString.fromLegacy("天|- 気|qì").segments)
    }

    @Test
    fun aozora() {
        val s = RubyString.fromAozora("｜青空《あおぞら》の下、漢字《かんじ》と時々《ときどき》。")
        assertEquals(
            listOf(seg("青空", "あおぞら"), seg("の下、"), seg("漢字", "かんじ"), seg("と"), seg("時々", "ときどき"), seg("。")),
            s.segments,
        )
        // No kanji before 《 and no ｜: literal.
        assertEquals("ひらがな《ruby》", RubyString.fromAozora("ひらがな《ruby》").text)
        // ｜ without ruby stays.
        assertEquals("a｜b", RubyString.fromAozora("a｜b").text)
        // Unclosed.
        assertEquals("漢字《かんじ", RubyString.fromAozora("漢字《かんじ").text)
    }
}
