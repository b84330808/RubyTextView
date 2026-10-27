package me.weilunli.rubytext.ja

import me.weilunli.rubytext.RubySegment
import kotlin.test.Test
import kotlin.test.assertEquals

class JapaneseAnnotatorTest {
    companion object {
        val ja = JapaneseAnnotator()
    }

    private fun seg(base: String, reading: String? = null) = RubySegment(base, reading)

    @Test
    fun okuriganaStaysBare() {
        assertEquals(listOf(seg("取", "と"), seg("り"), seg("扱", "あつか"), seg("い")), JapaneseAnnotator.align("取り扱い", "とりあつかい"))
        assertEquals(listOf(seg("食", "た"), seg("べる")), JapaneseAnnotator.align("食べる", "たべる"))
        assertEquals(listOf(seg("東京", "とうきょう")), JapaneseAnnotator.align("東京", "とうきょう"))
        // Kana that don't line up: one reading for the word.
        assertEquals(listOf(seg("見る", "みれ")), JapaneseAnnotator.align("見る", "みれ"))
        assertEquals(listOf(seg("お"), seg("茶", "ちゃ")), JapaneseAnnotator.align("お茶", "おちゃ"))
    }

    @Test
    fun katakanaToHiragana() {
        assertEquals("とうきょう", JapaneseAnnotator.toHiragana("トウキョウ"))
        assertEquals("ゔぁ", JapaneseAnnotator.toHiragana("ヴァ"))
        assertEquals("ー", JapaneseAnnotator.toHiragana("ー"))
    }

    @Test
    fun sentence() {
        val s = ja.annotate("東京へ行きます。")
        assertEquals("東京へ行きます。", s.text)
        val readings = s.segments.filter { it.hasRuby }.associate { it.base to it.reading }
        assertEquals("とうきょう", readings["東京"])
        assertEquals("い", readings["行"])
    }

    @Test
    fun kanaAndLatinStayPlain() {
        val s = ja.annotate("ひらがなとカタカナとABC")
        assertEquals(false, s.hasRuby)
        assertEquals("ひらがなとカタカナとABC", s.text)
    }
}
