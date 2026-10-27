package me.weilunli.rubytext.layout

import me.weilunli.rubytext.RubyString
import me.weilunli.rubytext.ZhuyinTone
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO

/** [RubyMeasurer] backed by java.awt fonts. Used for preview images only. */
class AwtMeasurer(private val fonts: Map<RunKind, Font>) : RubyMeasurer {
    private val frc = FontRenderContext(null, true, true)
    override fun width(kind: RunKind, text: String): Float =
        fonts.getValue(kind).getStringBounds(text, frc).width.toFloat()

    override fun metrics(kind: RunKind): FontMetrics {
        val font = fonts.getValue(kind)
        val lm = font.getLineMetrics("国", frc)
        return FontMetrics(font.size2D, lm.ascent, lm.descent)
    }
}

private val candidates = listOf(
    "Microsoft JhengHei", "Noto Sans CJK TC", "Noto Sans TC", "Source Han Sans TC", "PingFang TC",
    "Noto Sans CJK JP", "Yu Gothic", "MS Gothic",
)

private fun cjkFamily(): String {
    val available = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
    return candidates.firstOrNull { it in available && Font(it, Font.PLAIN, 12).canDisplayUpTo("你好ㄋㄧㄏㄠかな") == -1 }
        ?: Font.DIALOG
}

/** Draws [layout] into a new image. */
fun render(layout: RubyLayout, fonts: Map<RunKind, Font>, padding: Int = 16, guides: Boolean = true): BufferedImage {
    val w = layout.width.toInt() + padding * 2 + 1
    val h = layout.height.toInt() + padding * 2 + 1
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    g.color = Color.WHITE
    g.fillRect(0, 0, w, h)
    g.translate(padding, padding)
    if (guides) {
        g.color = Color(0xE8, 0xF0, 0xFF)
        for (c in layout.cells) g.draw(Rectangle2D.Float(c.left, c.top, c.right - c.left, c.bottom - c.top))
        g.color = Color(0xFF, 0xD8, 0xD8)
        for (line in layout.lines) g.draw(java.awt.geom.Line2D.Float(line.left, line.baseline, line.right, line.baseline))
    }
    for (run in layout.runs) {
        g.font = fonts.getValue(run.kind)
        g.color = when (run.kind) {
            RunKind.Base -> Color(0x20, 0x20, 0x20)
            RunKind.Ruby -> Color(0x1A, 0x5F, 0xB4)
            RunKind.Zhuyin -> Color(0xB0, 0x30, 0x30)
        }
        g.drawString(run.text, run.x, run.baseline)
    }
    g.color = Color(0xB0, 0x30, 0x30)
    for (t in layout.toneMarks) {
        if (t.tone == ZhuyinTone.Neutral) {
            g.fill(Ellipse2D.Float(t.left, t.top, t.width, t.height))
            continue
        }
        g.stroke = BasicStroke(t.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        for (stroke in ToneMarkShape.strokes(t.tone)) {
            val path = Path2D.Float()
            stroke.forEachIndexed { i, (x, y) ->
                val px = t.left + x * t.width
                val py = t.top + y * t.height
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            g.draw(path)
        }
    }
    g.dispose()
    return image
}

// java.awt only falls back to other fonts for logical fonts, so Latin with tone marks
// and Hebrew use Dialog.
private fun fonts(family: String, base: Float, rubyRatio: Float = 0.5f, zhuyinRatio: Float = 0.3f, baseFamily: String = family) = mapOf(
    RunKind.Base to Font(baseFamily, Font.PLAIN, 1).deriveFont(base),
    RunKind.Ruby to Font(Font.DIALOG, Font.PLAIN, 1).deriveFont(base * rubyRatio),
    RunKind.Zhuyin to Font(family, Font.PLAIN, 1).deriveFont(base * zhuyinRatio),
)

/** Writes preview PNGs to args[0]. */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "build/previews").apply { mkdirs() }
    val family = cjkFamily()
    println("Font: $family")

    fun draw(name: String, markup: String, size: Float, maxWidth: Float = Float.POSITIVE_INFINITY, options: RubyLayoutOptions = RubyLayoutOptions(), zhuyinRatio: Float = 0.3f, baseFamily: String = family) {
        val f = fonts(family, size, zhuyinRatio = zhuyinRatio, baseFamily = baseFamily)
        val layout = RubyLayoutEngine.layout(RubyString.parse(markup), AwtMeasurer(f), maxWidth, options)
        val file = File(out, "$name.png")
        ImageIO.write(render(layout, f), "png", file)
        println("${file.path}: ${layout.width.toInt()}x${layout.height.toInt()}, ${layout.lineCount} lines")
    }

    val zhuyin = "{我們|ㄨㄛˇ ˙ㄇㄣ}{是|ㄕˋ}{好|ㄏㄠˇ}{朋友|ㄆㄥˊ ˙ㄧㄡ}，{一|ㄧ}{起|ㄑㄧˇ}{學|ㄒㄩㄝˊ}{注音|ㄓㄨˋ ㄧㄣ}。"
    draw("zhuyin-right", zhuyin, 64f)
    draw("zhuyin-right-ratio-0.35", zhuyin, 64f, zhuyinRatio = 0.35f)
    draw("zhuyin-scale-24", zhuyin, 24f)
    draw("zhuyin-scale-96", "{小|ㄒㄧㄠˇ}{嗎|˙ㄇㄚ}{是|ㄕˋ}{人|ㄖㄣˊ}{三|ㄙㄢ}", 96f)
    draw("zhuyin-wrap", zhuyin + zhuyin, 64f, maxWidth = 600f)
    draw("zhuyin-over", zhuyin, 48f, options = RubyLayoutOptions(position = RubyPosition.Over))
    draw(
        "furigana",
        "{東京|とうきょう}の{空|そら}は{青|あお}い。{今日|きょう}は{承|うけたまわ}ります。{大人|おとな}と{子供|こ|ども}。",
        48f, options = RubyLayoutOptions(locale = Locale.JAPANESE),
    )
    draw(
        "furigana-wrap",
        "{吾輩|わがはい}は{猫|ねこ}である。{名前|なまえ}はまだ{無|な}い。どこで{生|う}まれたかとんと{見当|けんとう}がつかぬ。",
        40f, maxWidth = 420f, options = RubyLayoutOptions(locale = Locale.JAPANESE),
    )
    draw(
        "furigana-word-grouping",
        "{東京|とう|きょう}{都|と}",
        48f, options = RubyLayoutOptions(grouping = RubyGrouping.Word, locale = Locale.JAPANESE),
    )
    draw("pinyin", "{你好|nǐ hǎo}，{世界|shì jiè}！{北京|Běijīng}", 48f, options = RubyLayoutOptions(locale = Locale.CHINESE))
    draw("under", "{漢字|かんじ}を{下|した}に", 48f, options = RubyLayoutOptions(position = RubyPosition.Under))
    draw(
        "rtl",
        "שלום {你好|ㄋㄧˇ ㄏㄠˇ} עולם",
        40f, maxWidth = 500f, options = RubyLayoutOptions(direction = BaseDirection.ContentOrRtl), baseFamily = Font.DIALOG,
    )
}
