package me.weilunli.rubytext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import me.weilunli.rubytext.layout.BaseDirection
import me.weilunli.rubytext.layout.FontMetrics
import me.weilunli.rubytext.layout.LineAlign
import me.weilunli.rubytext.layout.RubyLayout
import me.weilunli.rubytext.layout.RubyLayoutEngine
import me.weilunli.rubytext.layout.RubyLayoutOptions
import me.weilunli.rubytext.layout.RubyMeasurer
import me.weilunli.rubytext.layout.RunKind
import me.weilunli.rubytext.layout.ToneMarkShape
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Text with ruby: Japanese furigana, Chinese Zhuyin (Bopomofo) or Pinyin.
 *
 * ```
 * RubyText(RubyString.parse("{東京|とうきょう}へ{行|い}く"))
 * RubyText(RubyString.parse("{你好|ㄋㄧˇ ㄏㄠˇ}"))        // Zhuyin on the right, textbook style
 * RubyText(RubyString.parse("{你好|nǐ hǎo}"))              // Pinyin above
 * ```
 *
 * A base and its reading are never split across lines. Screen readers get the base text,
 * or the base text with readings in parentheses when [readingsInSemantics] is true.
 *
 * @param style base text style; pass your theme's, e.g. `MaterialTheme.typography.bodyLarge`.
 *   Its `lineHeight` is ignored: lines are as tall as their content plus [RubyStyle.lineSpacing].
 * @param color base text color; overrides [style]'s color. Unspecified everywhere means black.
 * @param fontSize base font size; overrides [style]'s size. Unspecified everywhere means 14.sp.
 * @param rubyStyle how readings are placed and drawn.
 * @param textAlign line alignment. `Justify` is treated as `Start`.
 * @param onLayout receives every new layout, e.g. to hit-test taps with [RubyLayout.segmentIndexAt].
 */
@Composable
public fun RubyText(
    text: RubyString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    rubyStyle: RubyStyle = RubyStyle.Default,
    textAlign: TextAlign = TextAlign.Unspecified,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    readingsInSemantics: Boolean = false,
    onLayout: ((RubyLayout) -> Unit)? = null,
) {
    require(maxLines >= 1) { "maxLines must be at least 1" }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 0)

    val measurer = remember(text, style, color, fontSize, rubyStyle, density, layoutDirection, textMeasurer) {
        ComposeRubyMeasurer.create(textMeasurer, style, color, fontSize, rubyStyle, density, layoutDirection)
    }
    val options = remember(measurer, textAlign, softWrap, maxLines) {
        measurer.options(textAlign, softWrap, maxLines)
    }
    val holder = remember { LayoutHolder() }
    val semanticsText = remember(text, readingsInSemantics) {
        AnnotatedString(text.toPlainText(includeReadings = readingsInSemantics))
    }
    val currentOnLayout by rememberUpdatedState(onLayout)
    val policy = remember(text, measurer, options) {
        RubyMeasurePolicy(text, measurer, options, holder) { currentOnLayout?.invoke(it) }
    }

    Layout(
        content = {},
        modifier = modifier
            .semantics { this.text = semanticsText }
            .clipToBounds()
            .drawBehind { holder.drawn?.let { (layout, m) -> drawRubyLayout(layout, m) } },
        measurePolicy = policy,
    )
}

/** [RubyText] for brace markup: `RubyText("{東京|とうきょう}へ行く")`. See [RubyMarkup]. */
@Composable
public fun RubyText(
    markup: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    rubyStyle: RubyStyle = RubyStyle.Default,
    textAlign: TextAlign = TextAlign.Unspecified,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    readingsInSemantics: Boolean = false,
    onLayout: ((RubyLayout) -> Unit)? = null,
) {
    val parsed = remember(markup) { RubyString.parse(markup) }
    RubyText(
        text = parsed, modifier = modifier, style = style, color = color, fontSize = fontSize,
        rubyStyle = rubyStyle, textAlign = textAlign, softWrap = softWrap, maxLines = maxLines,
        readingsInSemantics = readingsInSemantics, onLayout = onLayout,
    )
}

/** Holds the last layout for drawing. Written during measure, read during draw. */
private class LayoutHolder {
    var drawn: Pair<RubyLayout, ComposeRubyMeasurer>? by mutableStateOf<Pair<RubyLayout, ComposeRubyMeasurer>?>(
        null,
        referentialEqualityPolicy(),
    )
}

private class RubyMeasurePolicy(
    private val text: RubyString,
    private val measurer: ComposeRubyMeasurer,
    private val options: RubyLayoutOptions,
    private val holder: LayoutHolder,
    private val onLayout: (RubyLayout) -> Unit,
) : MeasurePolicy {
    private var last: Triple<Float, Float, RubyLayout>? = null

    private fun layoutFor(maxWidth: Float, minWidth: Float): RubyLayout {
        last?.let { (w, mw, l) -> if (w == maxWidth && mw == minWidth) return l }
        return RubyLayoutEngine.layout(text, measurer, maxWidth, options, minWidth).also {
            last = Triple(maxWidth, minWidth, it)
        }
    }

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else Float.POSITIVE_INFINITY
        val layout = layoutFor(maxWidth, constraints.minWidth.toFloat())
        if (holder.drawn?.first !== layout) {
            holder.drawn = layout to measurer
            onLayout(layout)
        }
        val width = ceil(layout.width).toInt().coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = ceil(layout.height).toInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(
            width, height,
            mapOf<AlignmentLine, Int>(
                FirstBaseline to layout.firstBaseline.roundToInt(),
                LastBaseline to layout.lastBaseline.roundToInt(),
            ),
        ) {}
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        ceil(RubyLayoutEngine.layout(text, measurer, Float.POSITIVE_INFINITY, options).width).toInt()

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        ceil(RubyLayoutEngine.layout(text, measurer, 0f, options).width).toInt()

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        ceil(RubyLayoutEngine.layout(text, measurer, width.toFloat(), options).height).toInt()

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        minIntrinsicHeight(measurables, width)
}

private fun DrawScope.drawRubyLayout(layout: RubyLayout, m: ComposeRubyMeasurer) {
    for (run in layout.runs) {
        val result = m.result(run.kind, run.text)
        drawText(result, topLeft = Offset(run.x, run.baseline - result.firstBaseline))
    }
    if (layout.toneMarks.isEmpty()) return
    val color = m.zhuyinColor
    for (t in layout.toneMarks) {
        val strokes = ToneMarkShape.strokes(t.tone)
        if (strokes.isEmpty()) {
            drawCircle(color, radius = t.width / 2, center = Offset(t.centerX, t.centerY))
            continue
        }
        val stroke = Stroke(width = t.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        for (points in strokes) {
            val path = Path()
            points.forEachIndexed { i, (x, y) ->
                val px = t.left + x * t.width
                val py = t.top + y * t.height
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, color, style = stroke)
        }
    }
}

/** Measures runs with Compose's [TextMeasurer] and keeps the results for drawing. */
internal class ComposeRubyMeasurer private constructor(
    private val textMeasurer: TextMeasurer,
    private val styles: Map<RunKind, TextStyle>,
    private val sizes: Map<RunKind, Float>,
    private val measureDensity: Density,
    private val layoutDirection: LayoutDirection,
    private val rubyStyle: RubyStyle,
    private val basePx: Float,
    private val baseDirection: BaseDirection,
    private val locale: java.util.Locale,
    private val textAlignFromStyle: TextAlign,
    val zhuyinColor: Color,
) : RubyMeasurer {
    private data class Key(val kind: RunKind, val text: String)

    private val cache = HashMap<Key, TextLayoutResult>()
    private val metrics = HashMap<RunKind, FontMetrics>()

    fun result(kind: RunKind, text: String): TextLayoutResult = cache.getOrPut(Key(kind, text)) {
        textMeasurer.measure(
            AnnotatedString(text),
            style = styles.getValue(kind),
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxLines = 1,
            density = measureDensity,
            layoutDirection = layoutDirection,
        )
    }

    override fun width(kind: RunKind, text: String): Float =
        result(kind, text).multiParagraph.intrinsics.maxIntrinsicWidth

    override fun metrics(kind: RunKind): FontMetrics = metrics.getOrPut(kind) {
        // Latin with accents plus CJK: the line box covers both fonts.
        val r = result(kind, "Ág国あ")
        FontMetrics(sizes.getValue(kind), r.firstBaseline, r.multiParagraph.height - r.firstBaseline)
    }

    fun options(textAlign: TextAlign, softWrap: Boolean, maxLines: Int): RubyLayoutOptions {
        val align = if (textAlign == TextAlign.Unspecified) textAlignFromStyle else textAlign
        return RubyLayoutOptions(
            position = rubyStyle.position,
            align = rubyStyle.align,
            grouping = rubyStyle.grouping,
            rubyGap = resolve(rubyStyle.gap, 0f),
            zhuyinGap = resolve(rubyStyle.zhuyinGap, 0f),
            lineSpacing = resolve(rubyStyle.lineSpacing, 0f),
            lineAlign = when (align) {
                TextAlign.Left -> LineAlign.Left
                TextAlign.Right -> LineAlign.Right
                TextAlign.Center -> LineAlign.Center
                TextAlign.End -> LineAlign.End
                else -> LineAlign.Start
            },
            direction = baseDirection,
            softWrap = softWrap,
            maxLines = maxLines,
            showFirstTone = rubyStyle.showFirstTone,
            emBoxAscent = rubyStyle.emBoxAscent,
            locale = locale,
        )
    }

    private fun resolve(unit: TextUnit, default: Float): Float = resolve(unit, default, basePx, measureDensity)

    companion object {
        private fun resolve(unit: TextUnit, default: Float, basePx: Float, density: Density): Float = when {
            unit.type == TextUnitType.Unspecified -> default
            unit.isEm -> unit.value * basePx
            else -> with(density) { unit.toPx() }
        }

        fun create(
            textMeasurer: TextMeasurer,
            style: TextStyle,
            color: Color,
            fontSize: TextUnit,
            rubyStyle: RubyStyle,
            density: Density,
            layoutDirection: LayoutDirection,
        ): ComposeRubyMeasurer {
            val merged = style.merge(TextStyle(color = color, fontSize = fontSize))
            val baseColor = if (merged.color.isSpecified) merged.color else Color.Black
            // Base size with the user's (possibly non-linear) font scale applied once.
            val basePx = with(density) {
                when {
                    merged.fontSize.type == TextUnitType.Unspecified -> 14.sp.toPx()
                    merged.fontSize.isEm -> merged.fontSize.value * 14.sp.toPx()
                    else -> merged.fontSize.toPx()
                }
            }
            // Everything below is measured in pixels: sp == dp so sizes are not scaled twice.
            val linear = Density(density.density, 1f)
            fun Float.pxToSp(): TextUnit = (this / density.density).sp

            val rubyPx = resolve(rubyStyle.fontSize, 0.5f * basePx, basePx, density)
            val zhuyinPx = resolve(rubyStyle.zhuyinFontSize, 0.3f * basePx, basePx, density)
            val rubyColor = if (rubyStyle.color.isSpecified) rubyStyle.color else baseColor

            val base = merged.copy(
                color = baseColor,
                fontSize = basePx.pxToSp(),
                lineHeight = TextUnit.Unspecified,
                textAlign = TextAlign.Unspecified,
                textIndent = null,
            )
            fun reading(px: Float) = base.copy(
                color = rubyColor,
                fontSize = px.pxToSp(),
                fontFamily = rubyStyle.fontFamily ?: base.fontFamily,
                fontWeight = rubyStyle.fontWeight ?: base.fontWeight,
                letterSpacing = TextUnit.Unspecified,
                textDecoration = null,
                background = Color.Unspecified,
            )
            val styles = mapOf(RunKind.Base to base, RunKind.Ruby to reading(rubyPx), RunKind.Zhuyin to reading(zhuyinPx))
            val sizes = mapOf(RunKind.Base to basePx, RunKind.Ruby to rubyPx, RunKind.Zhuyin to zhuyinPx)

            val fallbackDirection = if (layoutDirection == LayoutDirection.Rtl) BaseDirection.ContentOrRtl else BaseDirection.ContentOrLtr
            val direction = when (merged.textDirection) {
                TextDirection.Ltr -> BaseDirection.Ltr
                TextDirection.Rtl -> BaseDirection.Rtl
                TextDirection.ContentOrLtr -> BaseDirection.ContentOrLtr
                TextDirection.ContentOrRtl -> BaseDirection.ContentOrRtl
                else -> fallbackDirection
            }
            val locale = merged.localeList?.firstOrNull()
                ?.let { java.util.Locale.forLanguageTag(it.toLanguageTag()) }
                ?: java.util.Locale.getDefault()

            return ComposeRubyMeasurer(
                textMeasurer, styles, sizes, linear, layoutDirection, rubyStyle, basePx, direction, locale,
                merged.textAlign, rubyColor,
            )
        }
    }
}
