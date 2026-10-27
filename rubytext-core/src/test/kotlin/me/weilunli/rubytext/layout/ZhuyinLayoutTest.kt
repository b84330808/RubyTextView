package me.weilunli.rubytext.layout

import me.weilunli.rubytext.RubyString
import me.weilunli.rubytext.ZhuyinTone
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Checks the right-side Zhuyin geometry against W3C clreq §5.5.3. Base 20, Zhuyin 6. */
class ZhuyinLayoutTest {
    private val m = FakeMeasurer(baseEm = 20f, rubyEm = 10f, zhuyinEm = 6f)
    private val eps = 0.001f
    private fun near(expected: Float, actual: Float, message: String = "") =
        assertTrue(abs(expected - actual) < eps, "$message expected $expected, got $actual")

    private fun layout(markup: String, options: RubyLayoutOptions = RubyLayoutOptions()) =
        RubyLayoutEngine.layout(RubyString.parse(markup), m, options = options)

    private fun TextRun.emTop() = baseline - 0.88f * 6f
    private fun TextRun.emBottom() = emTop() + 6f

    @Test
    fun autoPutsZhuyinOnTheRight() {
        val l = layout("{好|ㄏㄠˇ}")
        val base = l.runs.single { it.kind == RunKind.Base }
        val symbols = l.runs.filter { it.kind == RunKind.Zhuyin }
        assertEquals(listOf("ㄏ", "ㄠ"), symbols.map { it.text })
        // Column starts right after the character, symbols stack downward.
        assertTrue(symbols.all { it.x >= base.x + base.width - eps })
        near(symbols[0].x, symbols[1].x)
        near(symbols[0].emBottom(), symbols[1].emTop(), "symbols touch")
        assertEquals(0, l.runs.count { it.kind == RunKind.Ruby })
    }

    @Test
    fun stackIsCenteredOnTheCharacter() {
        val base = 20f
        for (markup in listOf("{一|ㄧ}", "{好|ㄏㄠˇ}", "{小|ㄒㄧㄠˇ}")) {
            val l = layout(markup)
            val b = l.runs.single { it.kind == RunKind.Base }
            val s = l.runs.filter { it.kind == RunKind.Zhuyin }
            val center = (s.first().emTop() + s.last().emBottom()) / 2
            near(b.baseline - (0.88f - 0.5f) * base, center, markup)
        }
    }

    @Test
    fun toneMarkSitsAtTheUpperRightOfTheLastSymbol() {
        for (markup in listOf("{是|ㄕˋ}", "{好|ㄏㄠˇ}", "{小|ㄒㄧㄠˇ}", "{人|ㄖㄣˊ}")) {
            val l = layout(markup)
            val s = l.runs.filter { it.kind == RunKind.Zhuyin }
            val tone = l.toneMarks.single()
            // Half of the mark is above the top of the last symbol.
            near(s.last().emTop(), tone.centerY, markup)
            // Right of the symbol column.
            assertTrue(tone.left >= s.last().x + 6f - eps, markup)
        }
        assertEquals(ZhuyinTone.Fourth, layout("{是|ㄕˋ}").toneMarks.single().tone)
        assertEquals(ZhuyinTone.Second, layout("{人|ㄖㄣˊ}").toneMarks.single().tone)
    }

    @Test
    fun firstToneHasNoMarkButKeepsTheSameWidth() {
        val toned = layout("{好|ㄏㄠˇ}")
        val plain = layout("{三|ㄙㄢ}")
        assertEquals(0, plain.toneMarks.size)
        near(toned.width, plain.width, "unmarked tones take the same space")
        assertEquals(0, layout("{三|ㄙㄢˉ}").toneMarks.size)
        assertEquals(1, layout("{三|ㄙㄢˉ}", RubyLayoutOptions(showFirstTone = true)).toneMarks.size)
    }

    @Test
    fun neutralDotGoesAboveTheFirstSymbol() {
        val l = layout("{嗎|˙ㄇㄚ}")
        val s = l.runs.filter { it.kind == RunKind.Zhuyin }
        val dot = l.toneMarks.single()
        assertEquals(ZhuyinTone.Neutral, dot.tone)
        assertTrue(dot.top + dot.height <= s.first().emTop() + eps, "dot above the column")
        near(s.first().x + 3f, dot.centerX, "dot centered on the symbols")
        // Written after the symbols, it's still drawn on top.
        val after = layout("{嗎|ㄇㄚ˙}")
        near(dot.centerY, after.toneMarks.single().centerY)
    }

    @Test
    fun columnWidthIsAboutHalfTheCharacter() {
        // clreq: annotations to the right take half the width of the base character.
        val l = layout("{好|ㄏㄠˇ}")
        val column = l.width - 20f
        assertTrue(column in 0.4f * 20f..0.6f * 20f, "column $column")
    }

    @Test
    fun wordOfPerCharacterReadings() {
        val l = layout("{你好|ㄋㄧˇ ㄏㄠˇ}嗎")
        assertEquals(listOf("你", "ㄋ", "ㄧ", "好", "ㄏ", "ㄠ", "嗎"), l.runs.map { it.text })
        assertEquals(2, l.toneMarks.size)
        // Each character carries its own column.
        val ni = l.runs.single { it.text == "你" }
        val hao = l.runs.single { it.text == "好" }
        // Cell = character (20) + symbol column (6) + tone slot (3).
        near(29f, hao.x - ni.x)
        near(29f * 2 + 20f, l.width)
    }

    @Test
    fun runTogetherZhuyinIsSplitPerCharacter() {
        val l = layout("{你好|ㄋㄧˇㄏㄠˇ}")
        assertEquals(2, l.toneMarks.size)
        assertEquals(listOf("你", "ㄋ", "ㄧ", "好", "ㄏ", "ㄠ"), l.runs.map { it.text })
    }

    @Test
    fun zhuyinLineDoesNotGrowTheLine() {
        // Three symbols at 0.3em fit beside the character: the line box stays the base's.
        val l = layout("{小|ㄒㄧㄠˇ}")
        near(20f, l.firstBaseline)
        near(25f, l.height)
    }

    @Test
    fun explicitOverWithZhuyin() {
        val l = layout("{你好|ㄋㄧˇ ㄏㄠˇ}", RubyLayoutOptions(position = RubyPosition.Over))
        assertEquals(listOf("ㄋㄧˇ", "ㄏㄠˇ"), l.runs.filter { it.kind == RunKind.Ruby }.map { it.text })
        assertEquals(0, l.toneMarks.size)
    }
}
