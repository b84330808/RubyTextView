package me.weilunli.rubytext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZhuyinPinyinTest {
    @Test
    fun parseSyllables() {
        assertEquals(ZhuyinSyllable("ㄏㄠ", ZhuyinTone.Third), Zhuyin.parseSyllable("ㄏㄠˇ"))
        assertEquals(ZhuyinSyllable("ㄇㄚ", ZhuyinTone.Neutral), Zhuyin.parseSyllable("˙ㄇㄚ"))
        assertEquals(ZhuyinSyllable("ㄇㄚ", ZhuyinTone.Neutral), Zhuyin.parseSyllable("ㄇㄚ˙"))
        assertEquals(ZhuyinSyllable("ㄓ", ZhuyinTone.First), Zhuyin.parseSyllable("ㄓ"))
        assertEquals(ZhuyinSyllable("ㄓ", ZhuyinTone.First), Zhuyin.parseSyllable("ㄓˉ"))
        assertEquals(ZhuyinSyllable("ㄒㄧㄠ", ZhuyinTone.Fourth), Zhuyin.parseSyllable("ㄒㄧㄠˋ"))
        assertEquals(ZhuyinSyllable("ㄕ", ZhuyinTone.Second), Zhuyin.parseSyllable(" ㄕˊ "))
        assertNull(Zhuyin.parseSyllable("ˇㄏㄠ"))
        assertNull(Zhuyin.parseSyllable("ㄏˇㄠ"))
        assertNull(Zhuyin.parseSyllable("ㄏㄠˇˇ"))
        assertNull(Zhuyin.parseSyllable("hao"))
        assertNull(Zhuyin.parseSyllable(""))
        assertEquals("˙ㄇㄚ", ZhuyinSyllable("ㄇㄚ", ZhuyinTone.Neutral).toString())
        assertEquals("ㄏㄠˇ", ZhuyinSyllable("ㄏㄠ", ZhuyinTone.Third).toString())
    }

    @Test
    fun detectsZhuyin() {
        assertTrue(Zhuyin.isZhuyin("ㄋㄧˇ ㄏㄠˇ"))
        assertTrue(Zhuyin.isZhuyin("ㆠㆤˊ")) // extended block
        assertFalse(Zhuyin.isZhuyin("nǐ hǎo"))
        assertFalse(Zhuyin.isZhuyin("ˇ"))
        assertFalse(Zhuyin.isZhuyin("ㄋㄧˇa"))
    }

    @Test
    fun splitsRunTogetherSyllables() {
        assertEquals(listOf("ㄋㄧˇ", "ㄏㄠˇ"), Zhuyin.splitSyllables("ㄋㄧˇㄏㄠˇ"))
        assertEquals(listOf("ㄓㄨㄥ", "ㄨㄣˊ"), Zhuyin.splitSyllables("ㄓㄨㄥㄨㄣˊ"))
        assertEquals(listOf("ㄕˋ", "˙ㄇㄚ"), Zhuyin.splitSyllables("ㄕˋ˙ㄇㄚ"))
        assertEquals(listOf("ㄇㄚ˙", "ㄇㄚ"), Zhuyin.splitSyllables("ㄇㄚ˙ㄇㄚ"))
        assertEquals(listOf("˙ㄇㄚ", "˙ㄇㄚ"), Zhuyin.splitSyllables("˙ㄇㄚ˙ㄇㄚ"))
        assertEquals(listOf("ㄒㄧ", "ㄢ"), Zhuyin.splitSyllables("ㄒㄧ ㄢ"))
        assertEquals(listOf("ㄦˊ", "ㄗ˙"), Zhuyin.splitSyllables("ㄦˊㄗ˙"))
    }

    @Test
    fun pinyinNumbersToMarks() {
        assertEquals("nǐ hǎo", Pinyin.fromNumbered("ni3 hao3"))
        assertEquals("lǜ", Pinyin.fromNumbered("lv4"))
        assertEquals("nǚ", Pinyin.fromNumbered("nu:3"))
        assertEquals("ma", Pinyin.fromNumbered("ma5"))
        assertEquals("ma", Pinyin.fromNumbered("ma0"))
        assertEquals("zhōngguó", Pinyin.fromNumbered("zhong1guo2"))
        assertEquals("dōu", Pinyin.fromNumbered("dou1"))
        assertEquals("guǐ", Pinyin.fromNumbered("gui3"))
        assertEquals("liù", Pinyin.fromNumbered("liu4"))
        assertEquals("Běijīng", Pinyin.fromNumbered("Bei3jing1"))
        assertEquals("ér", Pinyin.fromNumbered("er2"))
        assertEquals("no tones", Pinyin.fromNumbered("no tones"))
    }
}
