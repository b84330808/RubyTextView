package me.weilunli.rubytext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import me.weilunli.rubytext.layout.RubyGrouping
import me.weilunli.rubytext.layout.RubyPosition
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM screenshot tests (Robolectric native graphics + Roborazzi), no emulator.
 *
 * Record goldens: ./gradlew :rubytext-compose:recordRoborazziDebug
 * Verify:         ./gradlew :rubytext-compose:verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class RubyTextScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            Box(Modifier.background(Color.White).padding(8.dp)) { content() }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private val ja = TextStyle(fontSize = 28.sp, localeList = LocaleList("ja-JP"))
    private val zhTw = TextStyle(fontSize = 32.sp, localeList = LocaleList("zh-TW"))
    private val zhCn = TextStyle(fontSize = 28.sp, localeList = LocaleList("zh-CN"))

    private val zhuyin = "{我們|ㄨㄛˇ ˙ㄇㄣ}{是|ㄕˋ}{好|ㄏㄠˇ}{朋友|ㄆㄥˊ ˙ㄧㄡ}，{一|ㄧ}{起|ㄑㄧˇ}{學|ㄒㄩㄝˊ}{注音|ㄓㄨˋ ㄧㄣ}。"

    @Test
    fun furigana() = capture("furigana") {
        RubyText("{東京|とうきょう}の{空|そら}は{青|あお}い。{今日|きょう}は{承|うけたまわ}ります。", style = ja)
    }

    @Test
    fun furiganaWordGrouping() = capture("furigana_word_grouping") {
        Column {
            RubyText("{東京都|とう|きょう|と}", style = ja)
            RubyText("{東京都|とう|きょう|と}", style = ja, rubyStyle = RubyStyle(grouping = RubyGrouping.Word))
        }
    }

    @Test
    fun zhuyinRightWithTones() = capture("zhuyin_right") {
        RubyText(zhuyin, style = zhTw)
    }

    @Test
    fun zhuyinToneShapes() = capture("zhuyin_tones_large") {
        RubyText("{三|ㄙㄢ}{人|ㄖㄣˊ}{好|ㄏㄠˇ}{是|ㄕˋ}{嗎|˙ㄇㄚ}{小|ㄒㄧㄠˇ}", fontSize = 64.sp, style = zhTw)
    }

    @Test
    fun zhuyinAbove() = capture("zhuyin_over") {
        RubyText(zhuyin, style = zhTw, rubyStyle = RubyStyle(position = RubyPosition.Over, fontSize = 0.4.em))
    }

    @Test
    fun pinyin() = capture("pinyin") {
        RubyText("{你好|nǐ hǎo}，{世界|shì jiè}！{北京|Běijīng}", style = zhCn)
    }

    @Test
    fun wrapping() = capture("wrapping") {
        RubyText(
            "{吾輩|わがはい}は{猫|ねこ}である。{名前|なまえ}はまだ{無|な}い。どこで{生|う}まれたかとんと{見当|けんとう}がつかぬ。",
            modifier = Modifier.width(220.dp),
            style = ja,
        )
    }

    @Test
    fun zhuyinWrapping() = capture("zhuyin_wrapping") {
        RubyText(zhuyin + zhuyin, modifier = Modifier.width(260.dp), style = zhTw)
    }

    @Test
    fun fontScale200() = capture("font_scale_200") {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            Column {
                RubyText("{你好|ㄋㄧˇ ㄏㄠˇ}", style = TextStyle(fontSize = 20.sp))
                RubyText("{漢字|かんじ}", style = TextStyle(fontSize = 20.sp))
            }
        }
    }

    @Test
    fun rtlLayout() = capture("rtl") {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            RubyText("שלום {你好|ㄋㄧˇ ㄏㄠˇ} עולם", modifier = Modifier.width(300.dp), style = zhTw)
        }
    }

    @Test
    fun underAndColors() = capture("under_colors") {
        RubyText(
            "{漢字|かんじ}を{下|した}に",
            style = ja,
            color = Color(0xFF202020),
            rubyStyle = RubyStyle(position = RubyPosition.Under, color = Color(0xFF1A5FB4)),
        )
    }

    @Test
    fun html() = capture("html") {
        RubyText(RubyString.fromHtml("<ruby>漢<rt>かん</rt>字<rt>じ</rt></ruby>と<ruby>振<rp>(</rp><rt>ふ</rt><rp>)</rp></ruby>り<ruby>仮名<rt>がな</rt></ruby>"), style = ja)
    }
}
