package me.weilunli.rubytext.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weilunli.rubytext.RubyString
import me.weilunli.rubytext.RubyStyle
import me.weilunli.rubytext.RubyText
import me.weilunli.rubytext.layout.RubyGrouping
import me.weilunli.rubytext.layout.RubyLayout
import me.weilunli.rubytext.layout.RubyPosition

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Scaffold { padding -> Samples(Modifier.padding(padding)) }
            }
        }
    }
}

private data class Sample(val title: String, val text: RubyString, val locale: String)

private val samples = listOf(
    Sample(
        "注音（右側直排，含聲調）",
        RubyString.parse("{我們|ㄨㄛˇ ˙ㄇㄣ}{是|ㄕˋ}{好|ㄏㄠˇ}{朋友|ㄆㄥˊ ˙ㄧㄡ}，{一|ㄧ}{起|ㄑㄧˇ}{學|ㄒㄩㄝˊ}{注音|ㄓㄨˋ ㄧㄣ}。"),
        "zh-TW",
    ),
    Sample(
        "ふりがな",
        RubyString.parse("{吾輩|わがはい}は{猫|ねこ}である。{名前|なまえ}はまだ{無|な}い。どこで{生|う}まれたかとんと{見当|けんとう}がつかぬ。"),
        "ja-JP",
    ),
    Sample("Pinyin", RubyString.parse("{你好|nǐ hǎo}，{世界|shì jiè}！{我|wǒ}{在|zài}{北京|Běijīng}。"), "zh-CN"),
    Sample(
        "HTML <ruby>",
        RubyString.fromHtml("<ruby>漢<rt>かん</rt>字<rt>じ</rt></ruby>に<ruby>振<rp>(</rp><rt>ふ</rt><rp>)</rp></ruby>り<ruby>仮名<rt>がな</rt></ruby>"),
        "ja-JP",
    ),
    Sample("青空文庫", RubyString.fromAozora("｜青空《あおぞら》の下、時々《ときどき》雨。"), "ja-JP"),
)

@Composable
private fun Samples(modifier: Modifier = Modifier) {
    var width by remember { mutableFloatStateOf(1f) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    var position by remember { mutableStateOf(RubyPosition.Auto) }
    var grouping by remember { mutableStateOf(RubyGrouping.Character) }
    var tapped by remember { mutableStateOf("") }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Width ${(width * 100).toInt()}%")
        Slider(width, { width = it }, valueRange = 0.3f..1f)
        Text("Font scale ${"%.2f".format(fontScale)}")
        Slider(fontScale, { fontScale = it }, valueRange = 0.85f..2f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RubyPosition.entries.forEach {
                FilterChip(selected = position == it, onClick = { position = it }, label = { Text(it.name) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RubyGrouping.entries.forEach {
                FilterChip(selected = grouping == it, onClick = { grouping = it }, label = { Text(it.name) })
            }
        }
        if (tapped.isNotEmpty()) Text("Tapped: $tapped", style = MaterialTheme.typography.labelLarge)
        HorizontalDivider()

        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * fontScale)) {
            for (sample in samples) {
                Text(sample.title, style = MaterialTheme.typography.labelMedium)
                SampleText(sample, width, position, grouping) { tapped = it }
            }
        }
    }
}

@Composable
private fun SampleText(
    sample: Sample,
    width: Float,
    position: RubyPosition,
    grouping: RubyGrouping,
    onTap: (String) -> Unit,
) {
    // The last layout, for hit-testing taps. Not state: reading it never recomposes.
    val layout = remember { arrayOfNulls<RubyLayout>(1) }
    RubyText(
        text = sample.text,
        modifier = Modifier
            .fillMaxWidth(width)
            .pointerInput(sample) {
                detectTapGestures { offset ->
                    val index = layout[0]?.segmentIndexAt(offset.x, offset.y) ?: -1
                    if (index >= 0) {
                        val segment = sample.text.segments[index]
                        onTap(if (segment.hasRuby) "${segment.base} → ${segment.readings.joinToString(" ")}" else segment.base)
                    }
                }
            },
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = 24.sp,
            localeList = LocaleList(sample.locale),
        ),
        color = MaterialTheme.colorScheme.onSurface,
        rubyStyle = RubyStyle(
            position = position,
            grouping = grouping,
            color = MaterialTheme.colorScheme.primary,
        ),
        onLayout = { layout[0] = it },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SamplesPreview() {
    MaterialTheme { Samples() }
}
