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
import org.tomasino.stutter.settings.AppearanceOptions
import org.tomasino.stutter.settings.LanguageOptions
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.settings.SettingsRepository
import org.tomasino.stutter.settings.StutterOptions
import org.tomasino.stutter.settings.TextHandlingOptions
import org.tomasino.stutter.settings.settingsDataStore
import org.tomasino.stutter.settings.COLOR_SCHEME_OPTIONS
import org.tomasino.stutter.settings.applyColorScheme
import org.tomasino.stutter.settings.colorSchemeLabel
import org.tomasino.stutter.ui.theme.StutterAndroidTheme
import java.util.Locale

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
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val context = LocalContext.current
        Button(onClick = { (context as? android.app.Activity)?.finish() }) {
            Text("Back to Stutter")
        }

        SectionFrame(title = "Visual settings") {
            FloatSliderRow(
                label = "Base text size (sp)",
                value = options.appearance.baseTextSizeSp,
                min = 16f,
                max = 72f,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(baseTextSizeSp = newValue))
                }
            }
            FloatSliderRow(
                label = "Center scale",
                value = options.appearance.centerScale,
                min = 1.0f,
                max = 2.0f,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(centerScale = newValue))
                }
            }
        FloatSliderRow(
            label = "Letter spacing (em)",
            value = options.appearance.letterSpacingEm,
            min = -0.3f,
            max = 0.3f,
        ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(letterSpacingEm = newValue))
                }
            }
            FloatSliderRow(
                label = "Padding (dp)",
                value = options.appearance.paddingDp,
                min = 8f,
                max = 64f,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(paddingDp = newValue))
                }
            }
            SwitchRow(
                label = "Bold center letter",
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
                label = "Background color",
                colorValue = options.appearance.backgroundColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(backgroundColor = newValue))
                }
            }
            ColorFieldRow(
                label = "Left text color",
                colorValue = options.appearance.leftColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(leftColor = newValue))
                }
            }
            ColorFieldRow(
                label = "Center text color",
                colorValue = options.appearance.centerColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(centerColor = newValue))
                }
            }
            ColorFieldRow(
                label = "Remainder text color",
                colorValue = options.appearance.remainderColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(remainderColor = newValue))
                }
            }
            ColorFieldRow(
                label = "Flanker text color",
                colorValue = options.appearance.flankerColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(flankerColor = newValue))
                }
            }
            ColorFieldRow(
                label = "Button background color",
                colorValue = options.appearance.buttonBackgroundColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(buttonBackgroundColor = newValue))
                }
            }
            ColorFieldRow(
                label = "Button text color",
                colorValue = options.appearance.buttonTextColor,
            ) { newValue ->
                scope.launch {
                    repository.setAppearanceOptions(options.appearance.copy(buttonTextColor = newValue))
                }
            }

            Button(onClick = { resetTarget = ResetTarget.Visual }) {
                Text("Reset Visual Settings")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionFrame(title = "Timing & Features") {
            IntSliderRow(
                label = "WPM",
                value = options.playback.wpm,
                min = PlaybackOptions.MIN_WPM,
                max = PlaybackOptions.MAX_WPM,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(wpm = newValue))
                }
            }
            IntSliderRow(
                label = "Slow start count",
                value = options.playback.slowStartCount,
                min = PlaybackOptions.MIN_SLOW_START,
                max = PlaybackOptions.MAX_SLOW_START,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(slowStartCount = newValue))
                }
            }
            FloatSliderRow(
                label = "Sentence delay",
                value = options.playback.sentenceDelay,
                min = PlaybackOptions.MIN_SENTENCE_DELAY,
                max = PlaybackOptions.MAX_SENTENCE_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(sentenceDelay = newValue))
                }
            }
            FloatSliderRow(
                label = "Other punctuation delay",
                value = options.playback.otherPuncDelay,
                min = PlaybackOptions.MIN_OTHER_PUNC_DELAY,
                max = PlaybackOptions.MAX_OTHER_PUNC_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(otherPuncDelay = newValue))
                }
            }
            FloatSliderRow(
                label = "Short word delay",
                value = options.playback.shortWordDelay,
                min = PlaybackOptions.MIN_SHORT_WORD_DELAY,
                max = PlaybackOptions.MAX_SHORT_WORD_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(shortWordDelay = newValue))
                }
            }
            FloatSliderRow(
                label = "Long word delay",
                value = options.playback.longWordDelay,
                min = PlaybackOptions.MIN_LONG_WORD_DELAY,
                max = PlaybackOptions.MAX_LONG_WORD_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(longWordDelay = newValue))
                }
            }
            FloatSliderRow(
                label = "Numeric delay",
                value = options.playback.numericDelay,
                min = PlaybackOptions.MIN_NUMERIC_DELAY,
                max = PlaybackOptions.MAX_NUMERIC_DELAY,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(numericDelay = newValue))
                }
            }
            IntSliderRow(
                label = "Skip count",
                value = options.playback.skipCount,
                min = PlaybackOptions.MIN_SKIP_COUNT,
                max = PlaybackOptions.MAX_SKIP_COUNT,
            ) { newValue ->
                scope.launch {
                    repository.setPlaybackOptions(options.playback.copy(skipCount = newValue))
                }
            }

            IntSliderRow(
                label = "Max word length",
                value = options.textHandling.maxWordLength,
                min = TextHandlingOptions.MIN_MAX_WORD_LENGTH,
                max = TextHandlingOptions.MAX_MAX_WORD_LENGTH,
            ) { newValue ->
                scope.launch {
                    repository.setTextHandlingOptions(options.textHandling.copy(maxWordLength = newValue))
                }
            }
            SwitchRow(
                label = "Show flankers",
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
                label = "Auto-detect from HTML",
                checked = options.language.autoDetectFromHtml,
            ) { checked ->
                scope.launch {
                    repository.setLanguageOptions(options.language.copy(autoDetectFromHtml = checked))
                }
            }

            Button(onClick = { resetTarget = ResetTarget.TimingAndFeatures }) {
                Text("Reset Timing & Features")
            }
        }

        Button(onClick = { resetTarget = ResetTarget.All }) {
            Text("Reset All")
        }
        Text(
            text = "Font licenses: Atkinson Hyperlegible (Braille Institute, SIL OFL 1.1), " +
                "OpenDyslexic (Abelardo Gonzalez, SIL OFL 1.1), " +
                "IBM Plex Sans (IBM, SIL OFL 1.1), " +
                "Source Sans 3 and Source Serif 4 (Adobe, SIL OFL 1.1), " +
                "Noto Sans and Noto Serif (Google, SIL OFL 1.1), " +
                "Literata (Google, SIL OFL 1.1), " +
                "Merriweather Sans (Sorkin Type, SIL OFL 1.1), " +
                "Fira Sans (Mozilla, SIL OFL 1.1), " +
                "Iosevka (be5invis, SIL OFL 1.1), " +
                "Lexend (Google, SIL OFL 1.1). " +
                "Copyright (c) Braille Institute, Abelardo Gonzalez, IBM Corp, " +
                "Adobe Systems Incorporated, Google, Sorkin Type, Mozilla, be5invis. " +
                "License text: https://scripts.sil.org/OFL",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    resetTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { resetTarget = null },
            title = { Text(target.title) },
            text = { Text(target.message) },
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
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetTarget = null }) {
                    Text("Cancel")
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
        SectionHeader("Playback")
        Text("WPM: ${options.playback.wpm}")
        SectionHeader("Text handling")
        Text("Max word length: ${options.textHandling.maxWordLength}")
        SectionHeader("Language")
        Text("Auto-detect: ${options.language.autoDetectFromHtml}")
        SectionHeader("Appearance")
        Text("Base size: ${options.appearance.baseTextSizeSp}")
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
    onChange: (Int) -> Unit,
) {
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
            onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
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
                label = { Text("Hex color (#RRGGBB or #AARRGGBB)") },
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
                    .semantics { contentDescription = "$label preview" },
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
                Text(if (isPickerOpen) "Hide" else "Pick")
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
    val options = remember {
        listOf(
            "System default",
            "Atkinson Hyperlegible",
            "IBM Plex Sans",
            "Source Sans 3",
            "Source Serif 4",
            "Noto Sans",
            "Noto Serif",
            "Literata",
            "Merriweather Sans",
            "Fira Sans",
            "Iosevka",
            "Lexend",
            "Sans Serif",
            "Serif",
            "Monospace",
            "Cursive",
            "Sans Serif Condensed",
            "OpenDyslexic",
        )
    }
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selected) {
        null -> "System default"
        "atkinson-hyperlegible" -> "Atkinson Hyperlegible"
        "ibm-plex-sans" -> "IBM Plex Sans"
        "source-sans-3" -> "Source Sans 3"
        "source-serif-4" -> "Source Serif 4"
        "noto-sans" -> "Noto Sans"
        "noto-serif" -> "Noto Serif"
        "literata" -> "Literata"
        "merriweather-sans" -> "Merriweather Sans"
        "fira-sans" -> "Fira Sans"
        "iosevka" -> "Iosevka"
        "lexend" -> "Lexend"
        "sans-serif" -> "Sans Serif"
        "serif" -> "Serif"
        "monospace" -> "Monospace"
        "cursive" -> "Cursive"
        "sans-serif-condensed" -> "Sans Serif Condensed"
        "opendyslexic" -> "OpenDyslexic"
        else -> selected
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Font family")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Font family") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .semantics { contentDescription = "Font family" },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onSelected(
                                when (label) {
                                    "System default" -> null
                                    "Atkinson Hyperlegible" -> "atkinson-hyperlegible"
                                    "IBM Plex Sans" -> "ibm-plex-sans"
                                    "Source Sans 3" -> "source-sans-3"
                                    "Source Serif 4" -> "source-serif-4"
                                    "Noto Sans" -> "noto-sans"
                                    "Noto Serif" -> "noto-serif"
                                    "Literata" -> "literata"
                                    "Merriweather Sans" -> "merriweather-sans"
                                    "Fira Sans" -> "fira-sans"
                                    "Iosevka" -> "iosevka"
                                    "Lexend" -> "lexend"
                                    "Sans Serif" -> "sans-serif"
                                    "Serif" -> "serif"
                                    "Monospace" -> "monospace"
                                    "Cursive" -> "cursive"
                                    "Sans Serif Condensed" -> "sans-serif-condensed"
                                    "OpenDyslexic" -> "opendyslexic"
                                    else -> null
                                }
                            )
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
    val options = remember(deviceLocaleTag) {
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
            add(LanguageOption("Device default ($deviceLocaleTag)", null))
            addAll(localeOptions)
        }
    }
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.tag == selected }?.label
        ?: selected
        ?: "Device default ($deviceLocaleTag)"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Default language")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Default language") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .semantics { contentDescription = "Default language" },
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
    val selectedLabel = colorSchemeLabel(selected)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Color scheme")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Color scheme") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .semantics { contentDescription = "Color scheme" },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                COLOR_SCHEME_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
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
        Text("Tap color")
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
        Text("Hue")
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
        Text("Alpha")
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
        applyColorScheme(AppearanceOptions.DEFAULT, AppearanceOptions.DEFAULT.colorSchemeName, isDarkTheme)
    )
}

private enum class ResetTarget(val title: String, val message: String) {
    Visual(
        title = "Reset visual settings?",
        message = "This will restore the default appearance settings.",
    ),
    TimingAndFeatures(
        title = "Reset timing & features?",
        message = "This will restore the default timing, text handling, and language settings.",
    ),
    All(
        title = "Reset all settings?",
        message = "This will restore all settings to their defaults.",
    ),
}

private suspend fun resetAll(repository: SettingsRepository, isDarkTheme: Boolean) {
    repository.setPlaybackOptions(PlaybackOptions.DEFAULT)
    repository.setTextHandlingOptions(TextHandlingOptions.DEFAULT)
    repository.setLanguageOptions(LanguageOptions.DEFAULT)
    repository.setAppearanceOptions(
        applyColorScheme(AppearanceOptions.DEFAULT, AppearanceOptions.DEFAULT.colorSchemeName, isDarkTheme)
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
