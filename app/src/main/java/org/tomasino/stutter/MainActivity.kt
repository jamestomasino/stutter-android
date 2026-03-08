package org.tomasino.stutter

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.annotation.StringRes
import org.tomasino.stutter.settings.AppearanceOptions
import org.tomasino.stutter.settings.LanguageOptions
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.settings.SettingsRepository
import org.tomasino.stutter.settings.StutterOptions
import org.tomasino.stutter.settings.TextHandlingOptions
import org.tomasino.stutter.settings.settingsDataStore
import org.tomasino.stutter.settings.COLOR_SCHEME_OPTIONS
import org.tomasino.stutter.settings.DEFAULT_COLOR_SCHEME_ID
import org.tomasino.stutter.settings.applyColorScheme
import org.tomasino.stutter.settings.colorSchemeLabel
import org.tomasino.stutter.ui.theme.StutterAndroidTheme
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy {
        SettingsRepository(applicationContext.settingsDataStore, lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StutterAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(
                        repository = settingsRepository,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: SettingsRepository, modifier: Modifier = Modifier) {
    val options by repository.options.collectAsState()
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    var resetTarget by remember { mutableStateOf<ResetTarget?>(null) }
    var showDependencyLicenses by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val context = LocalContext.current
        Button(onClick = { (context as? android.app.Activity)?.finish() }) {
            Text(stringResource(R.string.back_to_stutter))
        }

        SectionFrame(title = stringResource(R.string.section_visual_settings)) {
            FloatSliderRow(
                label = stringResource(R.string.label_base_text_size),
                value = options.appearance.baseTextSizeSp,
                min = 16f,
                max = 72f,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(baseTextSizeSp = newValue))
                }
            }
            FloatSliderRow(
                label = stringResource(R.string.label_center_scale),
                value = options.appearance.centerScale,
                min = 1.0f,
                max = 2.0f,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(centerScale = newValue))
                }
            }
        FloatSliderRow(
            label = stringResource(R.string.label_letter_spacing),
            value = options.appearance.letterSpacingEm,
            min = -0.3f,
            max = 0.3f,
        ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(letterSpacingEm = newValue))
                }
            }
            FloatSliderRow(
                label = stringResource(R.string.label_padding),
                value = options.appearance.paddingDp,
                min = 8f,
                max = 64f,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(paddingDp = newValue))
                }
            }
            SwitchRow(
                label = stringResource(R.string.label_bold_center_letter),
                checked = options.appearance.boldCenter,
            ) { checked ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(boldCenter = checked))
                }
            }
            FontFamilyDropdown(
                selected = options.appearance.fontFamilyName,
                onSelected = { newValue ->
                    scope.launch {
                        repository.setAppearanceOptions(
                            options.appearance.copy(
                                fontFamilyName = newValue,
                                letterSpacingEm = 0f,
                            )
                        )
                    }
                },
            )
            ColorSchemeDropdown(
                selected = options.appearance.colorSchemeName,
            ) { schemeId ->
                scope.launch {
                    repository.setAppearanceOptions(
                        applyColorScheme(options.appearance, schemeId, isDarkTheme)
                    )
                }
            }
            ColorFieldRow(
                label = stringResource(R.string.label_background_color),
                colorValue = options.appearance.backgroundColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(backgroundColor = newValue))
                }
            }
            ColorFieldRow(
                label = stringResource(R.string.label_left_text_color),
                colorValue = options.appearance.leftColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(leftColor = newValue))
                }
            }
            ColorFieldRow(
                label = stringResource(R.string.label_center_text_color),
                colorValue = options.appearance.centerColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(centerColor = newValue))
                }
            }
            ColorFieldRow(
                label = stringResource(R.string.label_remainder_text_color),
                colorValue = options.appearance.remainderColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(remainderColor = newValue))
                }
            }
            ColorFieldRow(
                label = stringResource(R.string.label_flanker_text_color),
                colorValue = options.appearance.flankerColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(flankerColor = newValue))
                }
            }
            ColorFieldRow(
                label = stringResource(R.string.label_button_background_color),
                colorValue = options.appearance.buttonBackgroundColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(buttonBackgroundColor = newValue))
                }
            }
            ColorFieldRow(
                label = stringResource(R.string.label_button_text_color),
                colorValue = options.appearance.buttonTextColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(buttonTextColor = newValue))
                }
            }

            Button(onClick = { resetTarget = ResetTarget.Visual }) {
                Text(stringResource(R.string.button_reset_visual_settings))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionFrame(title = stringResource(R.string.section_timing_features)) {
            IntSliderRow(
                label = stringResource(R.string.label_wpm),
                value = options.playback.wpm,
                min = PlaybackOptions.MIN_WPM,
                max = PlaybackOptions.MAX_WPM,
                stepSize = 25,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(wpm = newValue))
                }
            }
            IntSliderRow(
                label = stringResource(R.string.label_slow_start_count),
                value = options.playback.slowStartCount,
                min = PlaybackOptions.MIN_SLOW_START,
                max = PlaybackOptions.MAX_SLOW_START,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(slowStartCount = newValue))
                }
            }
            FloatSliderRow(
                label = stringResource(R.string.label_paragraph_delay),
                value = options.playback.paragraphDelay,
                min = PlaybackOptions.MIN_PARAGRAPH_DELAY,
                max = PlaybackOptions.MAX_PARAGRAPH_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(paragraphDelay = newValue))
                }
            }
            FloatSliderRow(
                label = stringResource(R.string.label_sentence_delay),
                value = options.playback.sentenceDelay,
                min = PlaybackOptions.MIN_SENTENCE_DELAY,
                max = PlaybackOptions.MAX_SENTENCE_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(sentenceDelay = newValue))
                }
            }
            FloatSliderRow(
                label = stringResource(R.string.label_other_punctuation_delay),
                value = options.playback.otherPuncDelay,
                min = PlaybackOptions.MIN_OTHER_PUNC_DELAY,
                max = PlaybackOptions.MAX_OTHER_PUNC_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(otherPuncDelay = newValue))
                }
            }
            FloatSliderRow(
                label = stringResource(R.string.label_short_word_delay),
                value = options.playback.shortWordDelay,
                min = PlaybackOptions.MIN_SHORT_WORD_DELAY,
                max = PlaybackOptions.MAX_SHORT_WORD_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(shortWordDelay = newValue))
                }
            }
            FloatSliderRow(
                label = stringResource(R.string.label_long_word_delay),
                value = options.playback.longWordDelay,
                min = PlaybackOptions.MIN_LONG_WORD_DELAY,
                max = PlaybackOptions.MAX_LONG_WORD_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(longWordDelay = newValue))
                }
            }
            FloatSliderRow(
                label = stringResource(R.string.label_numeric_delay),
                value = options.playback.numericDelay,
                min = PlaybackOptions.MIN_NUMERIC_DELAY,
                max = PlaybackOptions.MAX_NUMERIC_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(numericDelay = newValue))
                }
            }
            IntSliderRow(
                label = stringResource(R.string.label_skip_count),
                value = options.playback.skipCount,
                min = PlaybackOptions.MIN_SKIP_COUNT,
                max = PlaybackOptions.MAX_SKIP_COUNT,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(skipCount = newValue))
                }
            }

            IntSliderRow(
                label = stringResource(R.string.label_max_word_length),
                value = options.textHandling.maxWordLength,
                min = TextHandlingOptions.MIN_MAX_WORD_LENGTH,
                max = TextHandlingOptions.MAX_MAX_WORD_LENGTH,
            ) { newValue ->
                scope.launch {
                    repository.setTextHandlingOptions(options.textHandling.copy(maxWordLength = newValue))
                }
            }
            SwitchRow(
                label = stringResource(R.string.label_show_flankers),
                checked = options.textHandling.showFlankers,
            ) { checked ->
                scope.launch {
                    repository.setTextHandlingOptions(options.textHandling.copy(showFlankers = checked))
                }
            }

            LanguageDropdown(
                selected = options.language.defaultLanguageTag,
                deviceLocaleTag = Locale.getDefault().toLanguageTag(),
            ) { newValue ->
                scope.launch {
                    repository.setLanguageOptions(options.language.copy(defaultLanguageTag = newValue))
                }
            }
            SwitchRow(
                label = stringResource(R.string.label_auto_detect_html),
                checked = options.language.autoDetectFromHtml,
            ) { checked ->
                scope.launch {
                    repository.setLanguageOptions(options.language.copy(autoDetectFromHtml = checked))
                }
            }

            Button(onClick = { resetTarget = ResetTarget.TimingAndFeatures }) {
                Text(stringResource(R.string.button_reset_timing_features))
            }
        }

        Button(onClick = { resetTarget = ResetTarget.All }) {
            Text(stringResource(R.string.button_reset_all))
        }
        Text(
            text = stringResource(R.string.text_font_licenses),
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = { showDependencyLicenses = !showDependencyLicenses }) {
            Text(stringResource(R.string.text_dependency_licenses_toggle))
        }
        if (showDependencyLicenses) {
            Text(
                text = stringResource(R.string.text_dependency_licenses),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    resetTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { resetTarget = null },
            title = { Text(stringResource(target.titleRes)) },
            text = { Text(stringResource(target.messageRes)) },
            confirmButton = {
                TextButton(onClick = {
                    resetTarget = null
                    scope.launch {
                        when (target) {
                            ResetTarget.Visual -> resetVisualSettings(repository, isDarkTheme)
                            ResetTarget.TimingAndFeatures -> resetTimingAndFeatures(repository)
                            ResetTarget.All -> resetAll(repository, isDarkTheme)
                        }
                    }
                }) {
                    Text(stringResource(R.string.dialog_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { resetTarget = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    StutterAndroidTheme {
        SettingsScreenPreviewContent()
    }
}

@Composable
private fun SettingsScreenPreviewContent() {
    val options = remember { StutterOptions.DEFAULT }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(stringResource(R.string.section_playback))
        Text(stringResource(R.string.preview_wpm, options.playback.wpm))
        SectionHeader(stringResource(R.string.section_text_handling))
        Text(stringResource(R.string.preview_max_word_length, options.textHandling.maxWordLength))
        SectionHeader(stringResource(R.string.section_language))
        Text(stringResource(R.string.preview_auto_detect, options.language.autoDetectFromHtml.toString()))
        SectionHeader(stringResource(R.string.section_appearance))
        Text(stringResource(R.string.preview_base_size, options.appearance.baseTextSizeSp.toString()))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun SectionFrame(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(title)
            content()
        }
    }
}

@Composable
private fun IntSliderRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    stepSize: Int? = null,
    onChange: (Int) -> Unit,
) {
    val normalizedStepSize = stepSize?.takeIf { it > 0 }
    val sliderSteps = normalizedStepSize
        ?.takeIf { (max - min) % it == 0 }
        ?.let { ((max - min) / it) - 1 }
        ?.coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Text(value.toString())
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { sliderValue ->
                val snappedValue = if (normalizedStepSize != null) {
                    val snappedSteps = ((sliderValue - min.toFloat()) / normalizedStepSize.toFloat())
                        .roundToInt()
                    (min + snappedSteps * normalizedStepSize).coerceIn(min, max)
                } else {
                    sliderValue.toInt().coerceIn(min, max)
                }
                onChange(snappedValue)
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = sliderSteps ?: 0,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun FloatSliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Text(String.format("%.2f", value))
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun ColorFieldRow(
    label: String,
    colorValue: Int,
    onColorChange: (Int) -> Unit,
) {
    var textValue by remember { mutableStateOf(colorValue.toHexString()) }
    var isEditing by remember { mutableStateOf(false) }
    var isPickerOpen by remember { mutableStateOf(false) }
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0f) }
    var value by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }
    val colorPreviewLabel = stringResource(R.string.label_color_preview, label)

    LaunchedEffect(colorValue) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(colorValue, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        alpha = AndroidColor.alpha(colorValue) / 255f
        if (!isEditing) {
            textValue = colorValue.toHexString()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    parseColorHex(newValue)?.let(onColorChange)
                },
                label = { Text(stringResource(R.string.label_hex_color)) },
                modifier = Modifier.weight(1f)
                    .onFocusChanged { isEditing = it.isFocused }
                    .semantics { contentDescription = label },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = colorPreviewLabel },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .background(Color(colorValue)),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { isPickerOpen = !isPickerOpen }) {
                Text(if (isPickerOpen) stringResource(R.string.button_hide) else stringResource(R.string.button_pick))
            }
        }

        if (isPickerOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SaturationValuePicker(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                ) { newSaturation, newValue ->
                    saturation = newSaturation
                    value = newValue
                    onColorChange(colorFromHsv(hue, saturation, value, alpha))
                }
                HuePicker(hue = hue) { newHue ->
                    hue = newHue
                    onColorChange(colorFromHsv(hue, saturation, value, alpha))
                }
                AlphaPicker(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    alpha = alpha,
                ) { newAlpha ->
                    alpha = newAlpha
                    onColorChange(colorFromHsv(hue, saturation, value, alpha))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontFamilyDropdown(
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    data class FontOption(val label: String, val value: String?)
    val systemDefaultLabel = stringResource(R.string.font_system_default)
    val sansSerifLabel = stringResource(R.string.font_sans_serif)
    val serifLabel = stringResource(R.string.font_serif)
    val monospaceLabel = stringResource(R.string.font_monospace)
    val cursiveLabel = stringResource(R.string.font_cursive)
    val sansSerifCondensedLabel = stringResource(R.string.font_sans_serif_condensed)
    val options = remember(
        systemDefaultLabel,
        sansSerifLabel,
        serifLabel,
        monospaceLabel,
        cursiveLabel,
        sansSerifCondensedLabel,
    ) {
        listOf(
            FontOption(systemDefaultLabel, null),
            FontOption("Atkinson Hyperlegible", "atkinson-hyperlegible"),
            FontOption("IBM Plex Sans", "ibm-plex-sans"),
            FontOption("Source Sans 3", "source-sans-3"),
            FontOption("Source Serif 4", "source-serif-4"),
            FontOption("Noto Sans", "noto-sans"),
            FontOption("Noto Serif", "noto-serif"),
            FontOption("Literata", "literata"),
            FontOption("Merriweather Sans", "merriweather-sans"),
            FontOption("Fira Sans", "fira-sans"),
            FontOption("Iosevka", "iosevka"),
            FontOption("Lexend", "lexend"),
            FontOption(sansSerifLabel, "sans-serif"),
            FontOption(serifLabel, "serif"),
            FontOption(monospaceLabel, "monospace"),
            FontOption(cursiveLabel, "cursive"),
            FontOption(sansSerifCondensedLabel, "sans-serif-condensed"),
            FontOption("OpenDyslexic", "opendyslexic"),
        )
    }
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selected }?.label
        ?: stringResource(R.string.font_system_default)

    val fontFamilyLabel = stringResource(R.string.label_font_family)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(fontFamilyLabel)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(fontFamilyLabel) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .semantics { contentDescription = fontFamilyLabel },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onSelected(option.value)
                        },
                    )
                }
            }
        }
    }
}

private data class LanguageOption(
    val label: String,
    val tag: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    selected: String?,
    deviceLocaleTag: String,
    onSelected: (String?) -> Unit,
) {
    val displayLocale = Locale.getDefault()
    val deviceDefaultLabel = stringResource(R.string.device_default_language, deviceLocaleTag)
    val options = remember(deviceLocaleTag, deviceDefaultLabel) {
        val localeOptions = Locale.getAvailableLocales()
            .asSequence()
            .mapNotNull { locale ->
                val tag = locale.toLanguageTag().takeIf { it.isNotBlank() && it != "und" } ?: return@mapNotNull null
                val label = locale.getDisplayName(displayLocale).takeIf { it.isNotBlank() } ?: tag
                LanguageOption(label = label, tag = tag)
            }
            .distinctBy { it.tag }
            .sortedBy { it.label.lowercase(displayLocale) }
            .toList()
        buildList(localeOptions.size + 1) {
            add(LanguageOption(deviceDefaultLabel, null))
            addAll(localeOptions)
        }
    }
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.tag == selected }?.label
        ?: selected
        ?: deviceDefaultLabel

    val defaultLanguageLabel = stringResource(R.string.label_default_language)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(defaultLanguageLabel)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(defaultLanguageLabel) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .semantics { contentDescription = defaultLanguageLabel },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onSelected(option.tag)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSchemeDropdown(
    selected: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedLabel = colorSchemeLabel(context, selected)

    val colorSchemeLabel = stringResource(R.string.label_color_scheme)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(colorSchemeLabel)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(colorSchemeLabel) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .semantics { contentDescription = colorSchemeLabel },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                COLOR_SCHEME_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            expanded = false
                            onSelected(option.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SaturationValuePicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
) {
    val hueColor = Color(colorFromHsv(hue, 1f, 1f, 1f))
    val indicatorColor = Color.White
    val indicatorStroke = Color.Black.copy(alpha = 0.4f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.label_tap_color))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(hue) {
                    detectTapGestures { offset ->
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        if (width == 0f || height == 0f) return@detectTapGestures
                        val x = offset.x.coerceIn(0f, width)
                        val y = offset.y.coerceIn(0f, height)
                        val newSaturation = (x / width).coerceIn(0f, 1f)
                        val newValue = (1f - (y / height)).coerceIn(0f, 1f)
                        onChange(newSaturation, newValue)
                    }
                }
                .pointerInput(hue) {
                    detectDragGestures { change, _ ->
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        if (width == 0f || height == 0f) return@detectDragGestures
                        val x = change.position.x.coerceIn(0f, width)
                        val y = change.position.y.coerceIn(0f, height)
                        val newSaturation = (x / width).coerceIn(0f, 1f)
                        val newValue = (1f - (y / height)).coerceIn(0f, 1f)
                        onChange(newSaturation, newValue)
                        change.consume()
                    }
                },
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val x = saturation.coerceIn(0f, 1f) * size.width
            val y = (1f - value.coerceIn(0f, 1f)) * size.height
            drawCircle(indicatorStroke, radius = 10f, center = Offset(x, y), style = Stroke(width = 3f))
            drawCircle(indicatorColor, radius = 10f, center = Offset(x, y), style = Stroke(width = 2f))
        }
    }
}

@Composable
private fun HuePicker(
    hue: Float,
    onChange: (Float) -> Unit,
) {
    val colors = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.label_hue))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val width = size.width.toFloat()
                        if (width == 0f) return@detectTapGestures
                        val x = offset.x.coerceIn(0f, width)
                        onChange((x / width * 360f).coerceIn(0f, 360f))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val width = size.width.toFloat()
                        if (width == 0f) return@detectDragGestures
                        val x = change.position.x.coerceIn(0f, width)
                        onChange((x / width * 360f).coerceIn(0f, 360f))
                        change.consume()
                    }
                },
        ) {
            drawRect(Brush.horizontalGradient(colors))
            val x = (hue.coerceIn(0f, 360f) / 360f) * size.width
            drawCircle(Color.White, radius = 8f, center = Offset(x, size.height / 2f), style = Stroke(width = 2f))
        }
    }
}

@Composable
private fun AlphaPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    alpha: Float,
    onChange: (Float) -> Unit,
) {
    val baseColor = Color(colorFromHsv(hue, saturation, value, 1f))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.label_alpha))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val width = size.width.toFloat()
                        if (width == 0f) return@detectTapGestures
                        val x = offset.x.coerceIn(0f, width)
                        onChange((x / width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val width = size.width.toFloat()
                        if (width == 0f) return@detectDragGestures
                        val x = change.position.x.coerceIn(0f, width)
                        onChange((x / width).coerceIn(0f, 1f))
                        change.consume()
                    }
                },
        ) {
            drawRect(Brush.horizontalGradient(listOf(baseColor.copy(alpha = 0f), baseColor)))
            val x = alpha.coerceIn(0f, 1f) * size.width
            drawCircle(Color.White, radius = 8f, center = Offset(x, size.height / 2f), style = Stroke(width = 2f))
        }
    }
}

private suspend fun resetTimingAndFeatures(repository: SettingsRepository) {
    repository.setPlaybackOptions(PlaybackOptions.DEFAULT)
    repository.setTextHandlingOptions(TextHandlingOptions.DEFAULT)
    repository.setLanguageOptions(LanguageOptions.DEFAULT)
}

private suspend fun resetVisualSettings(
    repository: SettingsRepository,
    isDarkTheme: Boolean,
) {
    repository.setAppearanceOptions(
        applyColorScheme(AppearanceOptions.DEFAULT, DEFAULT_COLOR_SCHEME_ID, isDarkTheme)
    )
}

private enum class ResetTarget(@StringRes val titleRes: Int, @StringRes val messageRes: Int) {
    Visual(
        titleRes = R.string.reset_visual_title,
        messageRes = R.string.reset_visual_message,
    ),
    TimingAndFeatures(
        titleRes = R.string.reset_timing_title,
        messageRes = R.string.reset_timing_message,
    ),
    All(
        titleRes = R.string.reset_all_title,
        messageRes = R.string.reset_all_message,
    ),
}

private suspend fun resetAll(repository: SettingsRepository, isDarkTheme: Boolean) {
    repository.setPlaybackOptions(PlaybackOptions.DEFAULT)
    repository.setTextHandlingOptions(TextHandlingOptions.DEFAULT)
    repository.setLanguageOptions(LanguageOptions.DEFAULT)
    repository.setAppearanceOptions(
        applyColorScheme(AppearanceOptions.DEFAULT, DEFAULT_COLOR_SCHEME_ID, isDarkTheme)
    )
}

private fun parseColorHex(value: String): Int? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val normalized = if (trimmed.startsWith("#")) trimmed.substring(1) else trimmed
    if (normalized.length != 6 && normalized.length != 8) return null
    val hex = normalized.uppercase()
    val colorLong = hex.toLongOrNull(16) ?: return null
    return if (hex.length == 6) {
        (0xFF shl 24) or colorLong.toInt()
    } else {
        colorLong.toInt()
    }
}

private fun Int.toHexString(): String = String.format("#%08X", this)

private fun colorFromHsv(hue: Float, saturation: Float, value: Float, alpha: Float): Int {
    val clampedAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    return AndroidColor.HSVToColor(clampedAlpha, floatArrayOf(hue, saturation, value))
}
