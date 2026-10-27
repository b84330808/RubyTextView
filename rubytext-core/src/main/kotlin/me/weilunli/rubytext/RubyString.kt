package me.weilunli.rubytext

/**
 * One run of base text with an optional reading (ruby).
 *
 * - No [readings]: plain text.
 * - One reading: the reading belongs to the whole [base] ("group ruby", e.g. 今日 → きょう).
 * - One reading per character of [base]: each reading belongs to one character
 *   ("mono ruby", e.g. 你好 → ㄋㄧˇ, ㄏㄠˇ). An empty string leaves that character bare.
 *
 * "Character" means a user-perceived character (an extended grapheme cluster), so
 * CJK Extension B ideographs and characters followed by variation selectors count as one.
 */
public class RubySegment(base: String, readings: List<String> = emptyList()) {
    public val base: String = base
    public val readings: List<String> = readings.toList()

    /** The base split into characters. */
    public val characters: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) { Graphemes.split(base) }

    init {
        require(this.readings.isEmpty() || base.isNotEmpty()) { "A reading needs base text" }
        if (this.readings.size > 1) {
            val count = Graphemes.count(base)
            require(this.readings.size == count) {
                "\"$base\" has $count characters but ${this.readings.size} readings: ${this.readings}"
            }
        }
    }

    /** One reading for the whole base. `null` and `""` mean no reading. */
    public constructor(base: String, reading: String?) :
        this(base, if (reading.isNullOrEmpty()) emptyList() else listOf(reading))

    public val hasRuby: Boolean get() = readings.any { it.isNotEmpty() }

    /** True when each character has its own reading (always true for a one-character base). */
    public val isMono: Boolean get() = readings.size > 1 || (readings.size == 1 && characters.size == 1)

    /** All readings joined, for group display. */
    public val reading: String get() = readings.joinToString("")

    override fun equals(other: Any?): Boolean =
        other is RubySegment && other.base == base && other.readings == readings

    override fun hashCode(): Int = base.hashCode() * 31 + readings.hashCode()

    override fun toString(): String =
        if (readings.isEmpty()) "RubySegment($base)" else "RubySegment($base|${readings.joinToString("|")})"
}

/**
 * Text with ruby annotations: an immutable list of [RubySegment]s.
 *
 * Build one with [buildRubyString], or parse markup with [RubyString.parse],
 * [RubyString.fromHtml] or [RubyString.fromAozora].
 */
public class RubyString(segments: List<RubySegment>) {
    public val segments: List<RubySegment> = segments.filter { it.base.isNotEmpty() }

    /** The base text without readings. */
    public val text: String by lazy(LazyThreadSafetyMode.PUBLICATION) { this.segments.joinToString("") { it.base } }

    public val hasRuby: Boolean get() = segments.any { it.hasRuby }

    /**
     * Plain text for screen readers, search or copying. With [includeReadings], each
     * annotated segment is followed by its reading in [open]/[close], e.g. `東京(とうきょう)`.
     * Mono readings are joined with [separator].
     */
    public fun toPlainText(
        includeReadings: Boolean = false,
        open: String = "(",
        close: String = ")",
        separator: String = " ",
    ): String {
        if (!includeReadings) return text
        return buildString {
            for (s in segments) {
                append(s.base)
                if (s.hasRuby) {
                    append(open)
                    append(s.readings.filter { it.isNotEmpty() }.joinToString(separator))
                    append(close)
                }
            }
        }
    }

    /** Brace markup that [parse] reads back to an equal [RubyString]. */
    public fun toMarkup(): String = RubyMarkup.format(this)

    public operator fun plus(other: RubyString): RubyString = RubyString(segments + other.segments)

    override fun equals(other: Any?): Boolean = other is RubyString && other.segments == segments
    override fun hashCode(): Int = segments.hashCode()
    override fun toString(): String = "RubyString(${toMarkup()})"

    public class Builder {
        private val segments = ArrayList<RubySegment>()

        /** Appends plain text. */
        public fun append(text: String): Builder = apply { if (text.isNotEmpty()) segments += RubySegment(text) }

        /** Appends [base] with one [reading] for the whole base. */
        public fun ruby(base: String, reading: String): Builder = apply { segments += RubySegment(base, reading) }

        /** Appends [base] with one reading per character. */
        public fun ruby(base: String, readings: List<String>): Builder =
            apply { segments += RubySegment(base, readings) }

        public fun append(segment: RubySegment): Builder = apply { segments += segment }
        public fun append(text: RubyString): Builder = apply { segments += text.segments }

        public fun build(): RubyString = RubyString(segments)
    }

    public companion object {
        public val Empty: RubyString = RubyString(emptyList())

        /** Parses brace markup: `{漢字|かんじ}`, `{東京|とう|きょう}`, `{你好|ㄋㄧˇ ㄏㄠˇ}`. See [RubyMarkup]. */
        public fun parse(markup: String): RubyString = RubyMarkup.parse(markup)

        /** Parses HTML `<ruby>` markup. See [HtmlRuby]. */
        public fun fromHtml(html: String): RubyString = HtmlRuby.parse(html)

        /** Parses Aozora Bunko ruby: `｜青空《あおぞら》`, `漢字《かんじ》`. See [RubyMarkup.parseAozora]. */
        public fun fromAozora(text: String): RubyString = RubyMarkup.parseAozora(text)

        /** Parses the v1 `RubyTextView` format (`令|れい 和|わ`). See [RubyMarkup.parseLegacy]. */
        public fun fromLegacy(combinedText: String): RubyString = RubyMarkup.parseLegacy(combinedText)
    }
}

/** Builds a [RubyString]: `buildRubyString { ruby("東京", "とうきょう"); append("へ") }`. */
public inline fun buildRubyString(block: RubyString.Builder.() -> Unit): RubyString =
    RubyString.Builder().apply(block).build()
