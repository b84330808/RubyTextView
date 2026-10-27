package me.weilunli.rubytext.layout

import me.weilunli.rubytext.RubySegment
import me.weilunli.rubytext.RubyString
import me.weilunli.rubytext.Scripts
import me.weilunli.rubytext.Zhuyin
import me.weilunli.rubytext.ZhuyinTone
import java.text.Bidi
import java.text.BreakIterator
import kotlin.math.max
import kotlin.math.min

/**
 * Lays out a [RubyString] into lines of positioned runs. Renderer-independent: give it a
 * [RubyMeasurer], then draw the [RubyLayout]'s runs and tone marks.
 *
 * Rules:
 * - A base and its reading are one unit: lines never wrap inside it. Segments with one reading
 *   per character may wrap between characters with [RubyGrouping.Character].
 * - Other wrap points come from [BreakIterator] for [RubyLayoutOptions.locale]
 *   (CJK line-breaking rules on Android's ICU), applied to the whole base text.
 * - Bidirectional text is reordered per line with [Bidi]; each unit is drawn as a whole.
 * - Zhuyin on the right follows W3C "Requirements for Chinese Text Layout" §5.5.3: a column
 *   of symbols centered on the character, tone marks `ˊ ˇ ˋ` beside the upper right corner
 *   of the last symbol (half above its top), the neutral-tone dot above the first symbol,
 *   and the same width reserved whether or not a syllable has a tone mark.
 */
public object RubyLayoutEngine {

    /** Zhuyin proportions, relative to the Zhuyin font size. */
    internal const val TONE_SLOT = 0.5f // width reserved for tone marks right of the symbols
    internal const val TONE_SIZE = 0.42f // tone mark box side
    internal const val TONE_STROKE = 0.1f
    internal const val NEUTRAL_SLOT = 0.3f // height reserved above the symbols for `˙`
    internal const val NEUTRAL_DOT = 0.2f // dot diameter

    /**
     * @param maxWidth wrap width; [Float.POSITIVE_INFINITY] for one line per paragraph.
     * @param minWidth the result is at least this wide; lines are aligned within it.
     */
    public fun layout(
        text: RubyString,
        measurer: RubyMeasurer,
        maxWidth: Float = Float.POSITIVE_INFINITY,
        options: RubyLayoutOptions = RubyLayoutOptions(),
        minWidth: Float = 0f,
    ): RubyLayout = Engine(text, measurer, options).run(if (options.softWrap) maxWidth else Float.POSITIVE_INFINITY, maxWidth, minWidth)
}

private class Element(val kind: RunKind, val text: String, val dx: Float, val dy: Float, val width: Float)

private class ToneElement(val tone: ZhuyinTone, val dx: Float, val dy: Float, val w: Float, val h: Float, val stroke: Float)

/** One unbreakable visual unit: plain text, or a base with its reading. */
private class Cell(
    val segmentIndex: Int,
    val start: Int, // offset in RubyString.text
    val end: Int,
    val level: Int, // bidi level
    var breakAfter: Boolean,
    val isPlain: Boolean,
    val text: String,
) {
    var width = 0f
    var trailingSpace = 0f
    var ascent = 0f
    var descent = 0f
    val elements = ArrayList<Element>(2)
    val tones = ArrayList<ToneElement>(0)
}

private class ParagraphItem(val segmentIndex: Int, val segment: RubySegment, val text: String, val start: Int)

private class Engine(val text: RubyString, val m: RubyMeasurer, val o: RubyLayoutOptions) {
    val base = m.metrics(RunKind.Base)
    val ruby by lazy { m.metrics(RunKind.Ruby) }
    val zhuyin by lazy { m.metrics(RunKind.Zhuyin) }

    val runs = ArrayList<TextRun>()
    val tones = ArrayList<ToneMark>()
    val cells = ArrayList<CellBox>()
    val lineBoxes = ArrayList<LineBox>()

    private class Line(val cells: List<Cell>, val rtl: Boolean)

    fun run(wrapWidth: Float, maxWidth: Float, minWidth: Float): RubyLayout {
        val lines = ArrayList<Line>()
        var exceeded = false
        for (paragraph in paragraphs()) {
            if (lines.size >= o.maxLines) { exceeded = true; break }
            val (paragraphCells, rtl) = buildCells(paragraph)
            for (lineCells in fill(paragraphCells, wrapWidth)) {
                if (lines.size >= o.maxLines) { exceeded = true; break }
                lines += Line(lineCells, rtl)
            }
        }
        if (lines.isEmpty()) lines += Line(emptyList(), isRtlDefault())

        val contentWidths = lines.map { line -> line.cells.sumOf { it.width.toDouble() }.toFloat() - trailing(line.cells) }
        var width = max(minWidth, contentWidths.maxOrNull() ?: 0f)
        if (maxWidth.isFinite()) width = min(width, max(maxWidth, minWidth))

        var y = 0f
        lines.forEachIndexed { index, line ->
            if (index > 0) y += o.lineSpacing
            y = place(line, contentWidths[index], width, y)
        }
        return RubyLayout(
            width = width,
            height = y,
            lines = lineBoxes,
            runs = runs,
            toneMarks = tones,
            cells = cells,
            didExceedMaxLines = exceeded,
            firstBaseline = lineBoxes.first().baseline,
            lastBaseline = lineBoxes.last().baseline,
        )
    }

    private fun trailing(cells: List<Cell>): Float = cells.lastOrNull()?.trailingSpace ?: 0f

    private fun isRtlDefault() = o.direction == BaseDirection.Rtl || o.direction == BaseDirection.ContentOrRtl

    // ---- 1. paragraphs ----

    private fun paragraphs(): List<List<ParagraphItem>> {
        val result = ArrayList<List<ParagraphItem>>()
        var current = ArrayList<ParagraphItem>()
        var offset = 0
        text.segments.forEachIndexed { index, segment ->
            if (segment.hasRuby || '\n' !in segment.base) {
                current += ParagraphItem(index, segment, segment.base, offset)
            } else {
                var from = 0
                while (true) {
                    val nl = segment.base.indexOf('\n', from)
                    val end = if (nl < 0) segment.base.length else nl
                    var part = segment.base.substring(from, end)
                    if (nl >= 0 && part.endsWith('\r')) part = part.dropLast(1)
                    if (part.isNotEmpty()) current += ParagraphItem(index, segment, part, offset + from)
                    if (nl < 0) break
                    result += current
                    current = ArrayList()
                    from = nl + 1
                }
            }
            offset += segment.base.length
        }
        result += current
        return result
    }

    // ---- 2. cells ----

    private fun buildCells(items: List<ParagraphItem>): Pair<List<Cell>, Boolean> {
        val paraText = items.joinToString("") { it.text }
        val n = paraText.length
        if (n == 0) return emptyList<Cell>() to isRtlDefault()

        // Bidi levels.
        val levels = IntArray(n)
        val rtl: Boolean
        val needsBidi = isRtlDefault() || Bidi.requiresBidi(paraText.toCharArray(), 0, n)
        if (needsBidi) {
            val flag = when (o.direction) {
                BaseDirection.Ltr -> Bidi.DIRECTION_LEFT_TO_RIGHT
                BaseDirection.Rtl -> Bidi.DIRECTION_RIGHT_TO_LEFT
                BaseDirection.ContentOrLtr -> Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT
                BaseDirection.ContentOrRtl -> Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
            }
            val bidi = Bidi(paraText, flag)
            for (i in 0 until n) levels[i] = bidi.getLevelAt(i)
            rtl = bidi.baseLevel and 1 == 1
        } else {
            rtl = false
        }

        // Line-break opportunities over the whole paragraph.
        val breakAt = BooleanArray(n + 1)
        val iterator = BreakIterator.getLineInstance(o.locale)
        iterator.setText(paraText)
        var b = iterator.first()
        while (b != BreakIterator.DONE) {
            if (b in 1 until n) breakAt[b] = true
            b = iterator.next()
        }

        val cells = ArrayList<Cell>()
        var p = 0 // paragraph offset of the current item
        for (item in items) {
            val itemEnd = p + item.text.length
            if (!item.segment.hasRuby) {
                // Split plain text at break opportunities and direction changes.
                var runStart = p
                for (i in p + 1..itemEnd) {
                    if (i == itemEnd || breakAt[i] || levels[i] != levels[i - 1]) {
                        val cell = Cell(
                            item.segmentIndex, item.start + (runStart - p), item.start + (i - p), levels[runStart],
                            breakAfter = i < n && breakAt[i], isPlain = true, text = paraText.substring(runStart, i),
                        )
                        measurePlain(cell)
                        cells += cell
                        runStart = i
                    }
                }
            } else {
                rubyCells(item, p, levels, breakAt, n, cells)
            }
            p = itemEnd
        }
        return cells to rtl
    }

    private fun rubyCells(item: ParagraphItem, p: Int, levels: IntArray, breakAt: BooleanArray, n: Int, out: MutableList<Cell>) {
        val segment = item.segment
        val chars = segment.characters
        val joined = segment.reading
        val position = when (o.position) {
            RubyPosition.Auto -> if (Zhuyin.isZhuyin(joined)) RubyPosition.Right else RubyPosition.Over
            else -> o.position
        }
        // Per-character readings, if any.
        var mono: List<String>? = when {
            segment.readings.size == chars.size -> segment.readings
            else -> null
        }
        if (mono == null && position == RubyPosition.Right && chars.size > 1) {
            val split = Zhuyin.splitSyllables(joined)
            if (split.size == chars.size) mono = split
        }
        val itemEnd = p + item.text.length
        val endBreak = itemEnd < n && breakAt[itemEnd]

        if (mono != null && (o.grouping == RubyGrouping.Character || position == RubyPosition.Right)) {
            // One cell per character; with Word grouping they are glued together.
            var off = 0
            chars.forEachIndexed { i, ch ->
                val cs = p + off
                val ce = cs + ch.length
                val last = i == chars.size - 1
                val cell = Cell(
                    item.segmentIndex, item.start + off, item.start + off + ch.length, levels[cs],
                    breakAfter = if (last) endBreak else (o.grouping == RubyGrouping.Character && breakAt[ce]),
                    isPlain = false, text = ch,
                )
                measureRuby(cell, ch, mono[i], position)
                out += cell
                off += ch.length
            }
        } else {
            val cell = Cell(
                item.segmentIndex, item.start, item.start + item.text.length, levels[p],
                breakAfter = endBreak, isPlain = false, text = item.text,
            )
            // A group reading can't be split per character: Right falls back to Over.
            val pos = if (position == RubyPosition.Right && chars.size > 1) RubyPosition.Over else position
            measureRuby(cell, item.text, joined, pos)
            out += cell
        }
    }

    // ---- 3. cell geometry (x from the cell's left edge, y from the baseline, down is +) ----

    private fun measurePlain(cell: Cell) {
        val w = m.width(RunKind.Base, cell.text)
        cell.width = w
        val trimmed = cell.text.trimEnd()
        cell.trailingSpace = if (trimmed.length == cell.text.length) 0f
        else w - (if (trimmed.isEmpty()) 0f else m.width(RunKind.Base, trimmed))
        cell.ascent = base.ascent
        cell.descent = base.descent
        cell.elements += Element(RunKind.Base, cell.text, 0f, 0f, w)
    }

    private fun measureRuby(cell: Cell, baseText: String, reading: String, position: RubyPosition) {
        cell.ascent = base.ascent
        cell.descent = base.descent
        if (reading.isEmpty()) {
            val w = m.width(RunKind.Base, baseText)
            cell.width = w
            cell.elements += Element(RunKind.Base, baseText, 0f, 0f, w)
            return
        }
        when (position) {
            RubyPosition.Right -> measureRight(cell, baseText, reading)
            RubyPosition.Under -> measureOverUnder(cell, baseText, reading, under = true)
            else -> measureOverUnder(cell, baseText, reading, under = false)
        }
    }

    private fun measureOverUnder(cell: Cell, baseText: String, reading: String, under: Boolean) {
        val bw = m.width(RunKind.Base, baseText)
        val rw = m.width(RunKind.Ruby, reading)
        val w = max(bw, rw)
        cell.width = w

        val rubyBaseline = if (under) {
            (1f - o.emBoxAscent) * base.emSize + o.rubyGap + ruby.ascent
        } else {
            -(o.emBoxAscent * base.emSize) - o.rubyGap - ruby.descent
        }
        place(cell, RunKind.Base, baseText, bw, w, 0f)
        place(cell, RunKind.Ruby, reading, rw, w, rubyBaseline)
        if (under) {
            cell.descent = max(base.descent, rubyBaseline + ruby.descent)
        } else {
            cell.ascent = max(base.ascent, -rubyBaseline + ruby.ascent)
        }
    }

    /** Places [text] (natural width [tw]) in a cell of width [w]: centered or spread out. */
    private fun place(cell: Cell, kind: RunKind, text: String, tw: Float, w: Float, dy: Float) {
        if (tw >= w - 0.01f) {
            cell.elements += Element(kind, text, 0f, dy, tw)
            return
        }
        if (o.align == RubyAlign.SpaceAround) {
            val units = justificationUnits(text)
            if (units.size >= 2) {
                val widths = units.map { m.width(kind, it) }
                val gap = (w - widths.sum()) / units.size
                if (gap >= 0f) {
                    var x = gap / 2
                    units.forEachIndexed { i, u ->
                        cell.elements += Element(kind, u, x, dy, widths[i])
                        x += widths[i] + gap
                    }
                    return
                }
            }
        }
        cell.elements += Element(kind, text, (w - tw) / 2, dy, tw)
    }

    /**
     * Zhuyin to the right of one character (clreq §5.5.3). With Zhuyin size z:
     * symbols stack in a column z wide; a slot 0.5z wide right of it holds tone marks,
     * centered on the top edge of the last symbol; the neutral dot sits above the first
     * symbol; the stack is centered on the character's em box.
     */
    private fun measureRight(cell: Cell, baseText: String, reading: String) {
        val bw = m.width(RunKind.Base, baseText)
        val z = zhuyin.emSize
        val syllable = Zhuyin.parseSyllable(reading)
        val symbols: List<String> = splitChars(syllable?.symbols ?: reading.filterNot { it.isWhitespace() })
        val tone = syllable?.tone ?: ZhuyinTone.First
        val drawTone = when (tone) {
            ZhuyinTone.First -> o.showFirstTone && reading.contains('ˉ')
            else -> true
        }

        val symbolWidths = symbols.map { m.width(RunKind.Zhuyin, it) }
        val columnWidth = max(z, symbolWidths.maxOrNull() ?: 0f)
        val slot = RubyLayoutEngine.TONE_SLOT * z
        val colLeft = bw + o.zhuyinGap
        cell.width = colLeft + columnWidth + slot
        cell.elements += Element(RunKind.Base, baseText, 0f, 0f, bw)

        val emCenter = -(o.emBoxAscent - 0.5f) * base.emSize
        val neutralSlot = if (tone == ZhuyinTone.Neutral) RubyLayoutEngine.NEUTRAL_SLOT * z else 0f
        val stackHeight = neutralSlot + symbols.size * z
        val stackTop = emCenter - stackHeight / 2
        var top = stackTop
        var bottom = stackTop + stackHeight

        if (tone == ZhuyinTone.Neutral) {
            val d = RubyLayoutEngine.NEUTRAL_DOT * z
            cell.tones += ToneElement(
                ZhuyinTone.Neutral, colLeft + (columnWidth - d) / 2, stackTop + (neutralSlot - d) / 2, d, d, 0f,
            )
        }
        symbols.forEachIndexed { i, s ->
            val emTop = stackTop + neutralSlot + i * z
            cell.elements += Element(
                RunKind.Zhuyin, s, colLeft + (columnWidth - symbolWidths[i]) / 2, emTop + o.emBoxAscent * z, symbolWidths[i],
            )
        }
        if (drawTone && tone != ZhuyinTone.Neutral && symbols.isNotEmpty()) {
            val size = RubyLayoutEngine.TONE_SIZE * z
            val lastTop = stackTop + neutralSlot + (symbols.size - 1) * z
            val toneTop = lastTop - size / 2
            cell.tones += ToneElement(
                tone, colLeft + columnWidth + (slot - size) / 2, toneTop, size, size, RubyLayoutEngine.TONE_STROKE * z,
            )
            top = min(top, toneTop)
            bottom = max(bottom, toneTop + size)
        }
        cell.ascent = max(base.ascent, -top)
        cell.descent = max(base.descent, bottom)
    }

    // ---- 4. line filling ----

    private fun fill(cells: List<Cell>, wrapWidth: Float): List<List<Cell>> {
        if (cells.isEmpty()) return listOf(emptyList())
        val lines = ArrayList<List<Cell>>()
        var line = ArrayList<Cell>()
        var lineWidth = 0f

        fun newLine() {
            lines += line; line = ArrayList(); lineWidth = 0f
        }

        var i = 0
        while (i < cells.size) {
            // A piece: cells up to the next break opportunity.
            var j = i
            while (j < cells.size - 1 && !cells[j].breakAfter) j++
            val piece = cells.subList(i, j + 1)
            val pieceWidth = piece.sumOf { it.width.toDouble() }.toFloat()
            val visible = pieceWidth - piece.last().trailingSpace
            if (line.isNotEmpty() && lineWidth + visible > wrapWidth) newLine()
            if (line.isEmpty() && visible > wrapWidth && piece.all { it.isPlain }) {
                // Emergency: a word longer than the line. Break it between characters.
                for (c in piece.flatMap { splitPlain(it) }) {
                    if (line.isNotEmpty() && lineWidth + c.width - c.trailingSpace > wrapWidth) newLine()
                    line += c; lineWidth += c.width
                }
            } else {
                line.addAll(piece); lineWidth += pieceWidth
            }
            i = j + 1
        }
        if (line.isNotEmpty()) lines += line
        return lines
    }

    private fun splitPlain(cell: Cell): List<Cell> {
        val chars = splitChars(cell.text)
        if (chars.size <= 1) return listOf(cell)
        var off = cell.start
        return chars.mapIndexed { index, ch ->
            Cell(cell.segmentIndex, off, off + ch.length, cell.level, breakAfter = index == chars.size - 1 && cell.breakAfter, isPlain = true, text = ch)
                .also { measurePlain(it); off += ch.length }
        }
    }

    // ---- 5. placement ----

    private fun place(line: Line, contentWidth: Float, width: Float, top: Float): Float {
        val logical = line.cells
        val visual: List<Cell> = if (logical.any { it.level != 0 }) {
            val array = logical.toTypedArray<Any>()
            val levels = ByteArray(logical.size) { logical[it].level.toByte() }
            Bidi.reorderVisually(levels, 0, array, 0, array.size)
            array.map { it as Cell }
        } else logical

        val align = when (o.lineAlign) {
            LineAlign.Start -> if (line.rtl) LineAlign.Right else LineAlign.Left
            LineAlign.End -> if (line.rtl) LineAlign.Left else LineAlign.Right
            else -> o.lineAlign
        }
        var x = when (align) {
            LineAlign.Right -> width - contentWidth
            LineAlign.Center -> (width - contentWidth) / 2
            else -> 0f
        }
        // Trailing spaces of an RTL line end up on its left: hang them outside.
        if (line.rtl && logical.isNotEmpty() && visual.first() === logical.last()) x -= logical.last().trailingSpace

        val ascent = if (logical.isEmpty()) base.ascent else logical.maxOf { it.ascent }
        val descent = if (logical.isEmpty()) base.descent else logical.maxOf { it.descent }
        val baseline = top + ascent
        val bottom = baseline + descent
        val left = x
        for (cell in visual) {
            for (e in cell.elements) runs += TextRun(e.kind, e.text, x + e.dx, baseline + e.dy, e.width, cell.segmentIndex)
            for (t in cell.tones) tones += ToneMark(t.tone, x + t.dx, baseline + t.dy, t.w, t.h, t.stroke, cell.segmentIndex)
            cells += CellBox(x, top, x + cell.width, bottom, cell.segmentIndex, cell.start, cell.end)
            x += cell.width
        }
        lineBoxes += LineBox(top, baseline, bottom, left, left + contentWidth)
        return bottom
    }
}

private val clusterRegex = Regex("\\X")

internal fun splitChars(s: String): List<String> =
    if (s.all { !it.isSurrogate() && Character.getType(it) != Character.NON_SPACING_MARK.toInt() && it != '‍' && it !in '︀'..'️' }) {
        s.map { it.toString() }
    } else {
        clusterRegex.findAll(s).map { it.value }.toList()
    }

/** Units that space-around may separate: each CJK character, each run of other non-space characters. */
internal fun justificationUnits(text: String): List<String> {
    val units = ArrayList<String>()
    val word = StringBuilder()
    for (g in splitChars(text)) {
        val cp = g.codePointAt(0)
        when {
            Character.isWhitespace(cp) -> {
                if (word.isNotEmpty()) { units += word.toString(); word.setLength(0) }
            }
            isCjk(cp) -> {
                if (word.isNotEmpty()) { units += word.toString(); word.setLength(0) }
                units += g
            }
            else -> word.append(g)
        }
    }
    if (word.isNotEmpty()) units += word.toString()
    return units
}

internal fun isCjk(cp: Int): Boolean = Scripts.isCjk(cp)
