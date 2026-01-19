package org.tomasino.stutter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tomasino.stutter.settings.AppearanceOptions
import org.tomasino.stutter.settings.LanguageOptions
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.settings.SettingsRepository
import org.tomasino.stutter.settings.StutterOptions
import org.tomasino.stutter.settings.TextHandlingOptions
import org.tomasino.stutter.settings.settingsDataStore
import org.tomasino.stutter.ui.theme.StutterAndroidTheme

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
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val context = LocalContext.current
        Button(onClick = { context.startActivity(android.content.Intent(context, ReaderActivity::class.java)) }) {
            Text("Open Reader")
        }

        SectionHeader("Playback")
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

        SectionHeader("Text handling")
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

        SectionHeader("Language")
        SwitchRow(
            label = "Auto-detect from HTML",
            checked = options.language.autoDetectFromHtml,
        ) { checked ->
            scope.launch {
                repository.setLanguageOptions(options.language.copy(autoDetectFromHtml = checked))
            }
        }
        OutlinedTextField(
            value = options.language.defaultLanguageTag.orEmpty(),
            onValueChange = { newValue ->
                scope.launch {
                    repository.setLanguageOptions(options.language.copy(defaultLanguageTag = newValue))
                }
            },
            label = { Text("Default language tag (BCP-47)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        SectionHeader("Appearance")
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
            min = 0f,
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

        Button(onClick = { scope.launch { resetDefaults(repository) } }) {
            Text("Reset to defaults")
        }
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
    Text(text = text)
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

private suspend fun resetDefaults(repository: SettingsRepository) {
    repository.setPlaybackOptions(PlaybackOptions.DEFAULT)
    repository.setTextHandlingOptions(TextHandlingOptions.DEFAULT)
    repository.setLanguageOptions(LanguageOptions.DEFAULT)
    repository.setAppearanceOptions(AppearanceOptions.DEFAULT)
}
