package me.weilunli.rubytext

/**
 * Script checks by code point range. `Character.UnicodeScript` needs Android API 24,
 * and this library supports API 21.
 */
internal object Scripts {
    fun isHan(cp: Int): Boolean =
        cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x323AF ||
            cp in 0xF900..0xFAFF || cp in 0x2F800..0x2FA1F || cp in 0x2E80..0x2FDF ||
            cp == 0x3005 || cp == 0x3007 || cp in 0x3021..0x3029 || cp in 0x3038..0x303B

    fun isKana(cp: Int): Boolean =
        cp in 0x3040..0x30FF || cp in 0x31F0..0x31FF || cp in 0xFF66..0xFF9F || cp in 0x1B000..0x1B16F

    fun isBopomofo(cp: Int): Boolean = cp in 0x3100..0x312F || cp in 0x31A0..0x31BF

    fun isHangul(cp: Int): Boolean =
        cp in 0xAC00..0xD7AF || cp in 0x1100..0x11FF || cp in 0x3130..0x318F || cp in 0xA960..0xA97F || cp in 0xD7B0..0xD7FF

    /** CJK characters and punctuation: each one is its own justification unit. */
    fun isCjk(cp: Int): Boolean =
        isHan(cp) || isKana(cp) || isBopomofo(cp) || isHangul(cp) ||
            cp in 0x3000..0x303F || cp in 0xFF00..0xFFEF || cp in 0x02C7..0x02D9
}
