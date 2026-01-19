package org.tomasino.stutter.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
) {
    val options: StateFlow<StutterOptions> = dataStore.data
        .map { prefs -> prefs.toStutterOptions() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), StutterOptions.DEFAULT)

    val isLoaded: StateFlow<Boolean> = dataStore.data
        .map { true }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    suspend fun setPlaybackOptions(options: PlaybackOptions) {
        val clamped = options.clamped()
        dataStore.edit { prefs ->
            prefs[Keys.WPM] = clamped.wpm
            prefs[Keys.SLOW_START_COUNT] = clamped.slowStartCount
            prefs[Keys.SENTENCE_DELAY] = clamped.sentenceDelay
            prefs[Keys.OTHER_PUNC_DELAY] = clamped.otherPuncDelay
            prefs[Keys.SHORT_WORD_DELAY] = clamped.shortWordDelay
            prefs[Keys.LONG_WORD_DELAY] = clamped.longWordDelay
            prefs[Keys.NUMERIC_DELAY] = clamped.numericDelay
            prefs[Keys.SKIP_COUNT] = clamped.skipCount
        }
    }

    suspend fun setTextHandlingOptions(options: TextHandlingOptions) {
        val clamped = options.clamped()
        dataStore.edit { prefs ->
            prefs[Keys.MAX_WORD_LENGTH] = clamped.maxWordLength
            prefs[Keys.SHOW_FLANKERS] = clamped.showFlankers
        }
    }

    suspend fun setLanguageOptions(options: LanguageOptions) {
        val normalized = options.normalized()
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_DETECT_HTML] = normalized.autoDetectFromHtml
            if (normalized.defaultLanguageTag == null) {
                prefs.remove(Keys.DEFAULT_LANGUAGE_TAG)
            } else {
                prefs[Keys.DEFAULT_LANGUAGE_TAG] = normalized.defaultLanguageTag
            }
        }
    }

    suspend fun setAppearanceOptions(options: AppearanceOptions) {
        dataStore.edit { prefs ->
            prefs[Keys.BASE_TEXT_SIZE_SP] = options.baseTextSizeSp
            prefs[Keys.CENTER_SCALE] = options.centerScale
            if (options.fontFamilyName == null) {
                prefs.remove(Keys.FONT_FAMILY)
            } else {
                prefs[Keys.FONT_FAMILY] = options.fontFamilyName
            }
            prefs[Keys.BACKGROUND_COLOR] = options.backgroundColor
            prefs[Keys.LEFT_COLOR] = options.leftColor
            prefs[Keys.CENTER_COLOR] = options.centerColor
            prefs[Keys.REMAINDER_COLOR] = options.remainderColor
            prefs[Keys.FLANKER_COLOR] = options.flankerColor
            prefs[Keys.LETTER_SPACING] = options.letterSpacingEm
            prefs[Keys.PADDING_DP] = options.paddingDp
            prefs[Keys.BOLD_CENTER] = options.boldCenter
        }
    }

    private fun Preferences.toStutterOptions(): StutterOptions {
        val playback = PlaybackOptions(
            wpm = this[Keys.WPM] ?: PlaybackOptions.DEFAULT.wpm,
            slowStartCount = this[Keys.SLOW_START_COUNT] ?: PlaybackOptions.DEFAULT.slowStartCount,
            sentenceDelay = this[Keys.SENTENCE_DELAY] ?: PlaybackOptions.DEFAULT.sentenceDelay,
            otherPuncDelay = this[Keys.OTHER_PUNC_DELAY] ?: PlaybackOptions.DEFAULT.otherPuncDelay,
            shortWordDelay = this[Keys.SHORT_WORD_DELAY] ?: PlaybackOptions.DEFAULT.shortWordDelay,
            longWordDelay = this[Keys.LONG_WORD_DELAY] ?: PlaybackOptions.DEFAULT.longWordDelay,
            numericDelay = this[Keys.NUMERIC_DELAY] ?: PlaybackOptions.DEFAULT.numericDelay,
            skipCount = this[Keys.SKIP_COUNT] ?: PlaybackOptions.DEFAULT.skipCount,
        ).clamped()

        val textHandling = TextHandlingOptions(
            maxWordLength = this[Keys.MAX_WORD_LENGTH] ?: TextHandlingOptions.DEFAULT.maxWordLength,
            showFlankers = this[Keys.SHOW_FLANKERS] ?: TextHandlingOptions.DEFAULT.showFlankers,
        ).clamped()

        val language = LanguageOptions(
            autoDetectFromHtml = this[Keys.AUTO_DETECT_HTML] ?: LanguageOptions.DEFAULT.autoDetectFromHtml,
            defaultLanguageTag = this[Keys.DEFAULT_LANGUAGE_TAG],
        ).normalized()

        val appearance = AppearanceOptions(
            baseTextSizeSp = this[Keys.BASE_TEXT_SIZE_SP] ?: AppearanceOptions.DEFAULT.baseTextSizeSp,
            centerScale = this[Keys.CENTER_SCALE] ?: AppearanceOptions.DEFAULT.centerScale,
            fontFamilyName = this[Keys.FONT_FAMILY],
            backgroundColor = this[Keys.BACKGROUND_COLOR] ?: AppearanceOptions.DEFAULT.backgroundColor,
            leftColor = this[Keys.LEFT_COLOR] ?: AppearanceOptions.DEFAULT.leftColor,
            centerColor = this[Keys.CENTER_COLOR] ?: AppearanceOptions.DEFAULT.centerColor,
            remainderColor = this[Keys.REMAINDER_COLOR] ?: AppearanceOptions.DEFAULT.remainderColor,
            flankerColor = this[Keys.FLANKER_COLOR] ?: AppearanceOptions.DEFAULT.flankerColor,
            letterSpacingEm = this[Keys.LETTER_SPACING] ?: AppearanceOptions.DEFAULT.letterSpacingEm,
            paddingDp = this[Keys.PADDING_DP] ?: AppearanceOptions.DEFAULT.paddingDp,
            boldCenter = this[Keys.BOLD_CENTER] ?: AppearanceOptions.DEFAULT.boldCenter,
        )

        return StutterOptions(
            playback = playback,
            textHandling = textHandling,
            language = language,
            appearance = appearance,
        )
    }

    private object Keys {
        val WPM = intPreferencesKey("wpm")
        val SLOW_START_COUNT = intPreferencesKey("slow_start_count")
        val SENTENCE_DELAY = floatPreferencesKey("sentence_delay")
        val OTHER_PUNC_DELAY = floatPreferencesKey("other_punc_delay")
        val SHORT_WORD_DELAY = floatPreferencesKey("short_word_delay")
        val LONG_WORD_DELAY = floatPreferencesKey("long_word_delay")
        val NUMERIC_DELAY = floatPreferencesKey("numeric_delay")
        val SKIP_COUNT = intPreferencesKey("skip_count")
        val MAX_WORD_LENGTH = intPreferencesKey("max_word_length")
        val SHOW_FLANKERS = booleanPreferencesKey("show_flankers")
        val AUTO_DETECT_HTML = booleanPreferencesKey("auto_detect_html")
        val DEFAULT_LANGUAGE_TAG = stringPreferencesKey("default_language_tag")
        val BASE_TEXT_SIZE_SP = floatPreferencesKey("base_text_size_sp")
        val CENTER_SCALE = floatPreferencesKey("center_scale")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val BACKGROUND_COLOR = intPreferencesKey("background_color")
        val LEFT_COLOR = intPreferencesKey("left_color")
        val CENTER_COLOR = intPreferencesKey("center_color")
        val REMAINDER_COLOR = intPreferencesKey("remainder_color")
        val FLANKER_COLOR = intPreferencesKey("flanker_color")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val PADDING_DP = floatPreferencesKey("padding_dp")
        val BOLD_CENTER = booleanPreferencesKey("bold_center")
    }
}
