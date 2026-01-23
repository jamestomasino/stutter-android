package org.tomasino.stutter.settings

data class PlaybackOptions(
    val wpm: Int,
    val slowStartCount: Int,
    val sentenceDelay: Float,
    val otherPuncDelay: Float,
    val shortWordDelay: Float,
    val longWordDelay: Float,
    val numericDelay: Float,
    val skipCount: Int,
) {
    fun clamped(): PlaybackOptions {
        return copy(
            wpm = clampInt(wpm, MIN_WPM, MAX_WPM),
            slowStartCount = clampInt(slowStartCount, MIN_SLOW_START, MAX_SLOW_START),
            sentenceDelay = clampFloat(sentenceDelay, MIN_SENTENCE_DELAY, MAX_SENTENCE_DELAY),
            otherPuncDelay = clampFloat(otherPuncDelay, MIN_OTHER_PUNC_DELAY, MAX_OTHER_PUNC_DELAY),
            shortWordDelay = clampFloat(shortWordDelay, MIN_SHORT_WORD_DELAY, MAX_SHORT_WORD_DELAY),
            longWordDelay = clampFloat(longWordDelay, MIN_LONG_WORD_DELAY, MAX_LONG_WORD_DELAY),
            numericDelay = clampFloat(numericDelay, MIN_NUMERIC_DELAY, MAX_NUMERIC_DELAY),
            skipCount = clampInt(skipCount, MIN_SKIP_COUNT, MAX_SKIP_COUNT),
        )
    }

    companion object {
        const val MIN_WPM = 100
        const val MAX_WPM = 1800
        const val MIN_SLOW_START = 1
        const val MAX_SLOW_START = 10
        const val MIN_SENTENCE_DELAY = 1f
        const val MAX_SENTENCE_DELAY = 10f
        const val MIN_OTHER_PUNC_DELAY = 1f
        const val MAX_OTHER_PUNC_DELAY = 10f
        const val MIN_SHORT_WORD_DELAY = 1f
        const val MAX_SHORT_WORD_DELAY = 10f
        const val MIN_LONG_WORD_DELAY = 1f
        const val MAX_LONG_WORD_DELAY = 10f
        const val MIN_NUMERIC_DELAY = 1f
        const val MAX_NUMERIC_DELAY = 10f
        const val MIN_SKIP_COUNT = 0
        const val MAX_SKIP_COUNT = 100

        val DEFAULT = PlaybackOptions(
            wpm = 400,
            slowStartCount = 5,
            sentenceDelay = 2.5f,
            otherPuncDelay = 1.5f,
            shortWordDelay = 1.3f,
            longWordDelay = 1.4f,
            numericDelay = 1.8f,
            skipCount = 10,
        )
    }
}

data class TextHandlingOptions(
    val maxWordLength: Int,
    val showFlankers: Boolean,
) {
    fun clamped(): TextHandlingOptions {
        return copy(maxWordLength = clampInt(maxWordLength, MIN_MAX_WORD_LENGTH, MAX_MAX_WORD_LENGTH))
    }

    companion object {
        const val MIN_MAX_WORD_LENGTH = 5
        const val MAX_MAX_WORD_LENGTH = 50

        val DEFAULT = TextHandlingOptions(
            maxWordLength = 13,
            showFlankers = false,
        )
    }
}

data class LanguageOptions(
    val autoDetectFromHtml: Boolean,
    val defaultLanguageTag: String?,
) {
    fun normalized(): LanguageOptions {
        val normalizedTag = defaultLanguageTag?.trim()?.ifBlank { null }
        return copy(defaultLanguageTag = normalizedTag)
    }

    companion object {
        val DEFAULT = LanguageOptions(
            autoDetectFromHtml = true,
            defaultLanguageTag = null,
        )
    }
}

data class AppearanceOptions(
    val baseTextSizeSp: Float,
    val centerScale: Float,
    val fontFamilyName: String?,
    val colorSchemeName: String?,
    val backgroundColor: Int,
    val leftColor: Int,
    val centerColor: Int,
    val remainderColor: Int,
    val flankerColor: Int,
    val buttonBackgroundColor: Int,
    val buttonTextColor: Int,
    val letterSpacingEm: Float,
    val paddingDp: Float,
    val boldCenter: Boolean,
) {
    companion object {
        val DEFAULT = AppearanceOptions(
            baseTextSizeSp = 36f,
            centerScale = 1.0f,
            fontFamilyName = "atkinson-hyperlegible",
            colorSchemeName = DEFAULT_COLOR_SCHEME_ID,
            backgroundColor = 0xFFFFFFFF.toInt(),
            leftColor = 0xFF6B6B6B.toInt(),
            centerColor = 0xFF111111.toInt(),
            remainderColor = 0xFF4A4A4A.toInt(),
            flankerColor = 0xFF9A9A9A.toInt(),
            buttonBackgroundColor = 0xFF0F6A6A.toInt(),
            buttonTextColor = 0xFFFFFFFF.toInt(),
            letterSpacingEm = 0f,
            paddingDp = 24f,
            boldCenter = false,
        )
    }
}

data class StutterOptions(
    val playback: PlaybackOptions,
    val textHandling: TextHandlingOptions,
    val language: LanguageOptions,
    val appearance: AppearanceOptions,
) {
    companion object {
        val DEFAULT = StutterOptions(
            playback = PlaybackOptions.DEFAULT,
            textHandling = TextHandlingOptions.DEFAULT,
            language = LanguageOptions.DEFAULT,
            appearance = AppearanceOptions.DEFAULT,
        )
    }
}

internal fun clampInt(value: Int, min: Int, max: Int): Int {
    return value.coerceIn(min, max)
}

internal fun clampFloat(value: Float, min: Float, max: Float): Float {
    return value.coerceIn(min, max)
}
