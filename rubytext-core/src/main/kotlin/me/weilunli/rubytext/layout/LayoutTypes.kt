package me.weilunli.rubytext.layout

import me.weilunli.rubytext.ZhuyinTone
import java.util.Locale

/** Where readings go relative to their base characters. */
public enum class RubyPosition {
    /** [Right] for Zhuyin readings, [Over] for everything else. */
    Auto,

    /** Above the base (furigana, Pinyin). */
    Over,

    /** Below the base. */
    Under,

    /**
     * In a vertical column to the right of each character, with tone marks beside the
     * column: the Taiwanese textbook layout for Zhuyin (CSS `ruby-position: inter-character`).
     */
    Right,
}

/** How a reading shorter than its base (or a base shorter than its reading) is spread out. */
public enum class RubyAlign {
    /** Centered. */
    Center,

    /**
     * Spread over the width with half-size gaps at both ends (CSS `ruby-align: space-around`,
     * like JIS X 4051). Only CJK characters and spaces are spread; Latin words stay intact.
     */
    SpaceAround,
}

/** How a segment with one reading per character is laid out. */
public enum class RubyGrouping {
    /** Each reading sits over its own character, and the line may wrap between characters. */
    Character,

    /** The readings are joined and centered over the whole word, which never wraps. */
    Word,
}

/** Paragraph direction. */
public enum class BaseDirection { Ltr, Rtl, ContentOrLtr, ContentOrRtl }

/** Horizontal alignment of each line. */
public enum class LineAlign { Start, End, Left, Right, Center }

/** What a [TextRun] draws. Renderers map each kind to a text style. */
public enum class RunKind {
    /** Base text. */
    Base,

    /** Readings placed [RubyPosition.Over] or [RubyPosition.Under]. */
    Ruby,

    /** Readings placed [RubyPosition.Right], drawn one character at a time. */
    Zhuyin,
}

/**
 * Font metrics for one [RunKind], in the same unit as widths (pixels, usually).
 * [ascent] and [descent] are positive distances from the baseline to the top and
 * bottom of the line box; [emSize] is the font size.
 */
public class FontMetrics(public val emSize: Float, public val ascent: Float, public val descent: Float) {
    override fun toString(): String = "FontMetrics(em=$emSize, ascent=$ascent, descent=$descent)"
}

/** Measures text for the layout engine. Implemented by each renderer (Compose, Views, AWT ...). */
public interface RubyMeasurer {
    /** Advance width of [text] drawn as one run in the style for [kind]. */
    public fun width(kind: RunKind, text: String): Float

    /** Metrics of the style for [kind]. */
    public fun metrics(kind: RunKind): FontMetrics
}

/**
 * Layout settings. Lengths are in the measurer's unit.
 *
 * @property emBoxAscent where the ideographic em box sits: its top is `emBoxAscent * fontSize`
 *   above the baseline. 0.88 matches Noto Sans CJK / Source Han and most CJK fonts.
 * @property showFirstTone draw `ˉ` for first-tone Zhuyin syllables that spell it out.
 *   Taiwanese convention leaves the first tone unmarked.
 */
public data class RubyLayoutOptions(
    val position: RubyPosition = RubyPosition.Auto,
    val align: RubyAlign = RubyAlign.SpaceAround,
    val grouping: RubyGrouping = RubyGrouping.Character,
    val rubyGap: Float = 0f,
    val zhuyinGap: Float = 0f,
    val lineSpacing: Float = 0f,
    val lineAlign: LineAlign = LineAlign.Start,
    val direction: BaseDirection = BaseDirection.ContentOrLtr,
    val softWrap: Boolean = true,
    val maxLines: Int = Int.MAX_VALUE,
    val showFirstTone: Boolean = false,
    val emBoxAscent: Float = 0.88f,
    val locale: Locale = Locale.getDefault(),
) {
    init {
        require(maxLines >= 1) { "maxLines must be at least 1" }
    }
}

/** A run of text to draw with the style for [kind], its left edge at [x] and baseline at [baseline]. */
public class TextRun(
    public val kind: RunKind,
    public val text: String,
    public val x: Float,
    public val baseline: Float,
    public val width: Float,
    /** Index into [me.weilunli.rubytext.RubyString.segments]. */
    public val segmentIndex: Int,
) {
    override fun toString(): String = "TextRun($kind \"$text\" x=$x baseline=$baseline w=$width seg=$segmentIndex)"
}

/**
 * A Zhuyin tone mark to draw inside the box ([left], [top], [width], [height]).
 * Renderers draw `ˊ ˇ ˋ ˉ` as strokes of [strokeWidth] that fill the box, and the
 * neutral tone `˙` as a dot inscribed in it.
 */
public class ToneMark(
    public val tone: ZhuyinTone,
    public val left: Float,
    public val top: Float,
    public val width: Float,
    public val height: Float,
    public val strokeWidth: Float,
    public val segmentIndex: Int,
) {
    public val centerX: Float get() = left + width / 2
    public val centerY: Float get() = top + height / 2

    override fun toString(): String = "ToneMark($tone left=$left top=$top ${width}x$height seg=$segmentIndex)"
}

/** One laid-out line. [left]/[right] bound its content, without trailing spaces. */
public class LineBox(
    public val top: Float,
    public val baseline: Float,
    public val bottom: Float,
    public val left: Float,
    public val right: Float,
)

/** The area of one base/ruby unit, for hit testing. Offsets index into `RubyString.text`. */
public class CellBox(
    public val left: Float,
    public val top: Float,
    public val right: Float,
    public val bottom: Float,
    public val segmentIndex: Int,
    public val textStart: Int,
    public val textEnd: Int,
) {
    public fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom
}

/** The result of [RubyLayoutEngine.layout]. Coordinates are physical: x grows to the right. */
public class RubyLayout(
    public val width: Float,
    public val height: Float,
    public val lines: List<LineBox>,
    public val runs: List<TextRun>,
    public val toneMarks: List<ToneMark>,
    public val cells: List<CellBox>,
    /** True when [RubyLayoutOptions.maxLines] cut off text. */
    public val didExceedMaxLines: Boolean,
    /** Baseline of the first line, for aligning with other text. */
    public val firstBaseline: Float,
    public val lastBaseline: Float,
) {
    public val lineCount: Int get() = lines.size

    /** The cell under ([x], [y]), or null. */
    public fun cellAt(x: Float, y: Float): CellBox? = cells.firstOrNull { it.contains(x, y) }

    /** The segment index under ([x], [y]), or -1. */
    public fun segmentIndexAt(x: Float, y: Float): Int = cellAt(x, y)?.segmentIndex ?: -1
}
