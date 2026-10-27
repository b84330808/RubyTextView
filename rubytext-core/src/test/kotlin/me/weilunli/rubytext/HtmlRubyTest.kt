package me.weilunli.rubytext

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlRubyTest {
    private fun seg(base: String, vararg readings: String) = RubySegment(base, readings.toList())

    @Test
    fun simpleGroup() {
        assertEquals(
            listOf(seg("漢字", "かんじ"), seg("を読む")),
            RubyString.fromHtml("<ruby>漢字<rt>かんじ</rt></ruby>を読む").segments,
        )
    }

    @Test
    fun rpIsIgnored() {
        assertEquals(
            listOf(seg("東京", "とうきょう")),
            RubyString.fromHtml("<ruby>東京<rp>(</rp><rt>とうきょう</rt><rp>)</rp></ruby>").segments,
        )
    }

    @Test
    fun interleavedPairsBecomeOneWord() {
        assertEquals(
            listOf(seg("漢字", "かん", "じ")),
            RubyString.fromHtml("<ruby>漢<rt>かん</rt>字<rt>じ</rt></ruby>").segments,
        )
    }

    @Test
    fun rbThenRt() {
        assertEquals(
            listOf(seg("漢字", "かん", "じ")),
            RubyString.fromHtml("<ruby><rb>漢</rb><rb>字</rb><rt>かん</rt><rt>じ</rt></ruby>").segments,
        )
        assertEquals(
            listOf(seg("東京", "とうきょう")),
            RubyString.fromHtml("<ruby><rb>東京</rb><rtc><rt>とうきょう</rt></rtc></ruby>").segments,
        )
    }

    @Test
    fun whitespaceInsideRubyIsDropped() {
        val html = """
            <ruby>
              你 <rt> ㄋㄧˇ </rt>
              好 <rt>ㄏㄠˇ</rt>
            </ruby>
        """.trimIndent()
        assertEquals(listOf(seg("你好", "ㄋㄧˇ", "ㄏㄠˇ")), RubyString.fromHtml(html).segments)
    }

    @Test
    fun otherTagsAndEntities() {
        val s = RubyString.fromHtml("<p>A &amp; <b>B</b>&#x3042;&#12354;</p><p>next<br/>line</p>")
        assertEquals("A & Bああ\nnext\nline", s.text)
    }

    @Test
    fun baseWithoutRtStaysPlain() {
        assertEquals(listOf(seg("abc")), RubyString.fromHtml("<ruby>abc</ruby>").segments)
        assertEquals(listOf(seg("x", "y"), seg("z")), RubyString.fromHtml("<ruby>x<rt>y</rt>z</ruby>").segments)
    }

    @Test
    fun spaceAfterRubyIsKept() {
        assertEquals("Peking University", RubyString.fromHtml("<ruby>Peking<rt>北京</rt></ruby> University").text)
    }

    @Test
    fun scriptAndCommentsAreSkipped() {
        assertEquals("ok", RubyString.fromHtml("<!-- <ruby>x<rt>y</rt></ruby> --><script>bad()</script>ok").text)
    }
}
