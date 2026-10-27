package me.weilunli.rubytext

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import me.weilunli.rubytext.layout.RubyLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RubyTextSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun screenReadersGetTheBaseText() {
        compose.setContent { RubyText("{東京|とうきょう}へ{行|い}く") }
        compose.onNodeWithText("東京へ行く").assertExists()
    }

    @Test
    fun readingsCanBeIncluded() {
        compose.setContent { RubyText("{東京|とうきょう}へ", readingsInSemantics = true) }
        compose.onNodeWithText("東京(とうきょう)へ").assertExists()
    }

    @Test
    fun layoutIsReportedAndWraps() {
        var layout: RubyLayout? = null
        compose.setContent {
            RubyText(
                "{我們|ㄨㄛˇ ˙ㄇㄣ}{是|ㄕˋ}{好|ㄏㄠˇ}{朋友|ㄆㄥˊ ˙ㄧㄡ}，{一|ㄧ}{起|ㄑㄧˇ}{學|ㄒㄩㄝˊ}{注音|ㄓㄨˋ ㄧㄣ}。",
                modifier = Modifier.width(120.dp),
                onLayout = { layout = it },
            )
        }
        compose.waitForIdle()
        val l = layout!!
        assertTrue("wraps into several lines", l.lineCount > 1)
        assertEquals("one tone mark per toned syllable", 9, l.toneMarks.size)
    }
}
