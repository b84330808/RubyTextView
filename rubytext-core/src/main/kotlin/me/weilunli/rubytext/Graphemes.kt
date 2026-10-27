package me.weilunli.rubytext

/** Splits text into user-perceived characters (extended grapheme clusters). */
internal object Graphemes {
    private val cluster = Regex("\\X")

    fun split(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        if (text.all { it.isSimple() }) return text.map { it.toString() }
        return cluster.findAll(text).map { it.value }.toList()
    }

    fun count(text: String): Int =
        if (text.all { it.isSimple() }) text.length else cluster.findAll(text).count()

    /** A BMP character that never joins with its neighbours. */
    private fun Char.isSimple(): Boolean =
        !isSurrogate() && this != '‍' && this !in '̀'..'ͯ' &&
            this !in '︀'..'️' && this !in '゙'..'゚' &&
            this !in 'ᄀ'..'ᇿ' && this !in 'ꥠ'..'꥿' && this !in 'ힰ'..'퟿' &&
            this != '\r' && !isCombiningOrFormat()

    private fun Char.isCombiningOrFormat(): Boolean {
        val type = Character.getType(this)
        return type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.FORMAT.toInt()
    }
}
