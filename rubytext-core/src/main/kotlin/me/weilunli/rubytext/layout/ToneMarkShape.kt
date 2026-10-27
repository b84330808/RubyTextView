package me.weilunli.rubytext.layout

import me.weilunli.rubytext.ZhuyinTone

/**
 * Stroke shapes for drawing Zhuyin tone marks at an exact position, independent of the
 * font. Each stroke is a polyline of points in a unit box (0..1, y down) that maps onto
 * [ToneMark]'s box. [ZhuyinTone.Neutral] has no strokes: draw a dot inscribed in the box.
 */
public object ToneMarkShape {
    private val second = listOf(listOf(0.3f to 0.92f, 0.72f to 0.08f))
    private val third = listOf(listOf(0.12f to 0.22f, 0.5f to 0.88f, 0.88f to 0.22f))
    private val fourth = listOf(listOf(0.28f to 0.08f, 0.7f to 0.92f))
    private val first = listOf(listOf(0.08f to 0.5f, 0.92f to 0.5f))

    /** Polylines for [tone]; empty for [ZhuyinTone.Neutral]. */
    public fun strokes(tone: ZhuyinTone): List<List<Pair<Float, Float>>> = when (tone) {
        ZhuyinTone.First -> first
        ZhuyinTone.Second -> second
        ZhuyinTone.Third -> third
        ZhuyinTone.Fourth -> fourth
        ZhuyinTone.Neutral -> emptyList()
    }
}
