package me.weilunli.rubytext.layout

import me.weilunli.rubytext.RubyString
import me.weilunli.rubytext.ZhuyinTone
import java.util.Locale
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Monospace-ish fake: CJK characters are 1em wide, spaces 0.25em, everything else 0.5em.
 * Line box: ascent 1.0em, descent 0.25em.
 */
class FakeMeasurer(val baseEm: Float = 20f, val rubyEm: Float = 10f, val zhuyinEm: Float = 6f) : RubyMeasurer {
    fun em(kind: RunKind) = when (kind) {
        RunKind.Base -> baseEm
        RunKind.Ruby -> rubyEm
        RunKind.Zhuyin -> zhuyinEm
    }

    override fun width(kind: RunKind, text: String): Float {
        var w = 0f
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            w += when {
                cp == ' '.code -> 0.25f
                isCjk(cp) -> 1f
                else -> 0.5f
            }
            i += Character.charCount(cp)
        }
        return w * em(kind)
    }

    override fun metrics(kind: RunKind) = FontMetrics(em(kind), em(kind) * 1.0f, em(kind) * 0.25f)
}

class RubyLayoutEngineTest {
    private val m = FakeMeasurer()
    private val eps = 0.001f
    private fun near(expected: Float, actual: Float, message: String? = null) =
        assertTrue(abs(expected - actual) < eps, "${message ?: ""} expected $expected, got $actual")

    private fun layout(markup: String, maxWidth: Float = Float.POSITIVE_INFINITY, options: RubyLayoutOptions = RubyLayoutOptions(align = RubyAlign.Center, locale = Locale.JAPANESE), measurer: RubyMeasurer = m) =
        RubyLayoutEngine.layout(RubyString.parse(markup), measurer, maxWidth, options)

    private fun RubyLayout.lineOf(run: TextRun) = lines.indexOfFirst { run.baseline in it.top..it.bottom }

    @Test
    fun groupRubyIsCenteredOverItsBase() {
        val l = layout("{漢字|かんじ}")
        val base = l.runs.single { it.kind == RunKind.Base }
        val ruby = l.runs.single { it.kind == RunKind.Ruby }
        near(40f, l.width) // base 2em = 40 > ruby 3 × 10 = 30
        near(0f, base.x)
        near(5f, ruby.x)
        // Ruby sits on top of the base's em box: its descent touches 0.88em above the base baseline.
        near(base.baseline - 0.88f * 20f - 0.25f * 10f, ruby.baseline)
        // The line grows to fit the ruby.
        near(0.88f * 20f + 0.25f * 10f + 10f, l.firstBaseline)
    }

    @Test
    fun spaceAroundSpreadsShortBase() {
        val l = layout("{大人|おとなたち}", options = RubyLayoutOptions(align = RubyAlign.SpaceAround)) // base 40, ruby 50
        val base = l.runs.filter { it.kind == RunKind.Base }
        assertEquals(listOf("大", "人"), base.map { it.text })
        // gap = (50 - 40) / 2 = 5: half gaps at the ends.
        near(2.5f, base[0].x)
        near(27.5f, base[1].x)
    }

    @Test
    fun spaceAroundSpreadsShortRuby() {
        val l = layout("{今日|きょ}", options = RubyLayoutOptions(align = RubyAlign.SpaceAround)) // base 40, ruby 2 × 10 = 20
        val ruby = l.runs.filter { it.kind == RunKind.Ruby }
        assertEquals(listOf("き", "ょ"), ruby.map { it.text })
        // gap = (40 - 20) / 2 = 10: half gaps (5) at the ends.
        near(5f, ruby[0].x)
        near(25f, ruby[1].x)
    }

    @Test
    fun spaceAroundKeepsLatinWordsWhole() {
        val l = layout("{北京市|Beijing}", options = RubyLayoutOptions(align = RubyAlign.SpaceAround)) // base 60, ruby 7 × 0.5 × 10 = 35
        val ruby = l.runs.filter { it.kind == RunKind.Ruby }
        assertEquals(listOf("Beijing"), ruby.map { it.text })
        near(12.5f, ruby[0].x)
    }

    @Test
    fun longRubyWidensTheCell() {
        val l = layout("{承|うけたまわ}る") // ruby 50 > base 20
        val base = l.runs.first { it.kind == RunKind.Base }
        near(15f, base.x)
        val okurigana = l.runs.first { it.text == "る" }
        near(50f, okurigana.x)
    }

    @Test
    fun underPutsRubyBelow() {
        val l = layout("{字|じ}", options = RubyLayoutOptions(position = RubyPosition.Under))
        val base = l.runs.single { it.kind == RunKind.Base }
        val ruby = l.runs.single { it.kind == RunKind.Ruby }
        near(base.baseline + 0.12f * 20f + 10f, ruby.baseline)
        assertTrue(l.lines[0].bottom >= ruby.baseline + 2.5f - eps)
    }

    @Test
    fun wrappingNeverSplitsAGroup() {
        // 4em of plain text, then a 2em group. 5em wide: the group can't fit on line 1.
        val l = layout("あいうえ{東京|とうきょう}", maxWidth = 100f)
        assertEquals(2, l.lineCount)
        val tokyo = l.runs.single { it.text == "東京" }
        val reading = l.runs.filter { it.kind == RunKind.Ruby }
        assertEquals(1, l.lineOf(tokyo))
        assertTrue(reading.all { l.lineOf(it) == 1 }, "reading stays with its base")
        near(5f, tokyo.x) // centered under the wider reading
        near(0f, l.lines[1].left)
    }

    @Test
    fun characterGroupingWrapsBetweenCharactersButKeepsPairs() {
        val l = layout("あいうえ{東京|とう|きょう}", maxWidth = 100f)
        assertEquals(2, l.lineCount)
        val to = l.runs.single { it.text == "東" }
        val kyo = l.runs.single { it.text == "京" }
        assertEquals(0, l.lineOf(to))
        assertEquals(1, l.lineOf(kyo))
        assertEquals(0, l.lineOf(l.runs.single { it.text == "とう" }))
        assertEquals(1, l.lineOf(l.runs.single { it.text == "きょう" }))
    }

    @Test
    fun wordGroupingKeepsTheWordTogether() {
        val opts = RubyLayoutOptions(grouping = RubyGrouping.Word, align = RubyAlign.Center, locale = Locale.JAPANESE)
        val l = layout("あいうえ{東京|とう|きょう}", maxWidth = 100f, options = opts)
        assertEquals(2, l.lineCount)
        assertEquals(1, l.lineOf(l.runs.single { it.text == "東京" }))
        // Word grouping joins the readings over the whole word.
        assertEquals(listOf("とうきょう"), l.runs.filter { it.kind == RunKind.Ruby }.map { it.text })
    }

    @Test
    fun hardLineBreaks() {
        val l = layout("あ\nい\r\n\nう")
        assertEquals(4, l.lineCount)
        assertEquals(listOf("あ", "い", "う"), l.runs.map { it.text })
    }

    @Test
    fun longLatinWordBreaksAsLastResort() {
        val l = layout("abcdefghij", maxWidth = 45f) // 10 × 10 = 100
        assertEquals(3, l.lineCount)
        assertTrue(l.lines.all { it.right - it.left <= 45f + eps })
    }

    @Test
    fun spacesHangAtLineEnd() {
        val l = layout("ab cd", maxWidth = 20f) // "ab " = 25 incl. space, "cd" = 20
        assertEquals(2, l.lineCount)
        near(20f, l.width)
    }

    @Test
    fun maxLines() {
        val l = layout("a\nb\nc", options = RubyLayoutOptions(maxLines = 2))
        assertEquals(2, l.lineCount)
        assertTrue(l.didExceedMaxLines)
    }

    @Test
    fun alignment() {
        val opts = RubyLayoutOptions(lineAlign = LineAlign.Center)
        val l = RubyLayoutEngine.layout(RubyString.parse("あ\nあい"), m, options = opts)
        near(40f, l.width)
        near(10f, l.runs.first { it.text == "あ" }.x)
        val end = RubyLayoutEngine.layout(RubyString.parse("あ"), m, 100f, RubyLayoutOptions(lineAlign = LineAlign.End), minWidth = 100f)
        near(80f, end.runs.single().x)
    }

    @Test
    fun scalesLinearlyWithFontSize() {
        val text = "{你好|ㄋㄧˇ ㄏㄠˇ}，{今日|きょう}"
        val a = RubyLayoutEngine.layout(RubyString.parse(text), FakeMeasurer(20f, 10f, 6f))
        val b = RubyLayoutEngine.layout(RubyString.parse(text), FakeMeasurer(40f, 20f, 12f))
        near(a.width * 2, b.width)
        near(a.height * 2, b.height)
        a.runs.zip(b.runs).forEach { (x, y) ->
            near(x.x * 2, y.x, x.text); near(x.baseline * 2, y.baseline, x.text)
        }
        a.toneMarks.zip(b.toneMarks).forEach { (x, y) -> near(x.top * 2, y.top); near(x.left * 2, y.left) }
    }

    @Test
    fun rtlParagraph() {
        // Hebrew words are reordered right to left; the line is right-aligned by Start.
        val l = RubyLayoutEngine.layout(
            RubyString.parse("אב גד"), m, 200f,
            RubyLayoutOptions(direction = BaseDirection.ContentOrLtr), minWidth = 200f,
        )
        val first = l.runs.first { it.text.startsWith("אב") }
        val second = l.runs.first { it.text.startsWith("גד") }
        assertTrue(first.x > second.x, "first word is on the right")
        near(200f, l.lines[0].right)
    }

    @Test
    fun rubyInsideRtlLayoutKeepsCjkOrder() {
        val l = RubyLayoutEngine.layout(
            RubyString.parse("{你好|ㄋㄧˇ ㄏㄠˇ}"), m, 300f,
            RubyLayoutOptions(direction = BaseDirection.Rtl), minWidth = 300f,
        )
        val ni = l.runs.single { it.text == "你" }
        val hao = l.runs.single { it.text == "好" }
        assertTrue(ni.x < hao.x, "CJK stays left to right")
        assertTrue(l.lines[0].right > 299f, "right-aligned in an RTL paragraph")
    }

    @Test
    fun hitTesting() {
        val l = layout("あ{東京|とうきょう}い")
        val tokyo = l.runs.single { it.text == "東京" }
        assertEquals(1, l.segmentIndexAt(tokyo.x + 1f, tokyo.baseline - 1f))
        assertEquals(-1, l.segmentIndexAt(-5f, 0f))
        val cell = l.cellAt(tokyo.x + 1f, tokyo.baseline - 1f)!!
        assertEquals("東京", RubyString.parse("あ{東京|とうきょう}い").text.substring(cell.textStart, cell.textEnd))
    }

    @Test
    fun emptyText() {
        val l = layout("")
        assertEquals(1, l.lineCount)
        near(0f, l.width)
        near(25f, l.height)
    }
}
