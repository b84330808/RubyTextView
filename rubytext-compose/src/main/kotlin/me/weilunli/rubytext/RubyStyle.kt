package me.weilunli.rubytext

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import me.weilunli.rubytext.layout.RubyAlign
import me.weilunli.rubytext.layout.RubyGrouping
import me.weilunli.rubytext.layout.RubyPosition

/**
 * How [RubyText] draws readings. Sizes in `em` are relative to the base font size, so
 * they follow the user's font scale along with the base text.
 *
 * @property position where readings go. [RubyPosition.Auto] puts Zhuyin on the right of
 *   each character (Taiwanese textbook style) and everything else above.
 * @property align how a reading narrower than its base is spread (and the reverse).
 * @property grouping for segments with one reading per character: wrap and align per
 *   character, or keep the word together.
 * @property fontSize size of readings placed above or below. Default `0.5.em`.
 * @property zhuyinFontSize size of Zhuyin placed on the right. Default `0.3.em`, the 3:10
 *   ratio of W3C clreq §5.5.3, which lets three symbols fit beside one character.
 * @property color reading color; unspecified uses the text color.
 * @property fontFamily reading font; null uses the base font.
 * @property gap space between base and readings placed above or below.
 * @property zhuyinGap space between a character and its Zhuyin column.
 * @property lineSpacing extra space between lines.
 * @property showFirstTone draw `ˉ` when a first-tone reading spells it out (unusual in Taiwan).
 * @property emBoxAscent top of the font's ideographic em box above the baseline, as a
 *   fraction of the font size. 0.88 fits Noto Sans CJK and most CJK fonts.
 */
@Immutable
public data class RubyStyle(
    val position: RubyPosition = RubyPosition.Auto,
    val align: RubyAlign = RubyAlign.SpaceAround,
    val grouping: RubyGrouping = RubyGrouping.Character,
    val fontSize: TextUnit = 0.5.em,
    val zhuyinFontSize: TextUnit = 0.3.em,
    val color: Color = Color.Unspecified,
    val fontFamily: FontFamily? = null,
    val fontWeight: FontWeight? = null,
    val gap: TextUnit = 0.em,
    val zhuyinGap: TextUnit = 0.em,
    val lineSpacing: TextUnit = 0.em,
    val showFirstTone: Boolean = false,
    val emBoxAscent: Float = 0.88f,
) {
    public companion object {
        public val Default: RubyStyle = RubyStyle()
    }
}
