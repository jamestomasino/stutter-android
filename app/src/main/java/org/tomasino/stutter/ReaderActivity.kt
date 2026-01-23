package org.tomasino.stutter

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.tomasino.stutter.hyphenation.PatternHyphenator
import org.tomasino.stutter.language.BasicLanguageResolver
import org.tomasino.stutter.reader.ReaderView
import org.tomasino.stutter.fetcher.FetchResult
import org.tomasino.stutter.fetcher.OkHttpFetcher
import org.tomasino.stutter.extractor.BasicExtractor
import org.tomasino.stutter.extractor.ExtractResult
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.settings.SettingsRepository
import org.tomasino.stutter.settings.settingsDataStore
import org.tomasino.stutter.tokenizer.IcuTokenizer
import org.tomasino.stutter.tokenizer.Token
import org.tomasino.stutter.tokenizer.buildTokensForText
import org.tomasino.stutter.ui.theme.StutterAndroidTheme
import java.util.Locale

class ReaderActivity : ComponentActivity() {
    private val settingsRepository by lazy {
        SettingsRepository(applicationContext.settingsDataStore, lifecycleScope)
    }
    private val sharedTextState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedTextState.value = extractText(intent)
        setContent {
            StutterAndroidTheme {
                ReaderScreen(settingsRepository, sharedTextState.value)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        sharedTextState.value = extractText(intent)
    }

    companion object {
        const val EXTRA_TEXT = "extra_text"
    }

    private fun extractText(intent: android.content.Intent?): String? {
        if (intent == null) return null
        val explicit = intent.getStringExtra(EXTRA_TEXT)
        if (!explicit.isNullOrBlank()) return explicit
        if (intent.action == android.content.Intent.ACTION_SEND) {
            if (intent.type?.startsWith("text/") == true) {
                return intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            }
        }
        return null
    }
}

@Composable
private fun ReaderScreen(repository: SettingsRepository, initialText: String?) {
    val options by repository.options.collectAsState()
    val isSettingsLoaded by repository.isLoaded.collectAsState()
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    val scheduler = remember { org.tomasino.stutter.scheduler.SchedulerImpl(scope) }
    val hyphenator = remember { PatternHyphenator() }
    val tokenizer = remember { IcuTokenizer() }
    val languageResolver = remember { BasicLanguageResolver() }
    val fetcher = remember { OkHttpFetcher() }
    val extractor = remember { BasicExtractor() }
    val clipboardManager = LocalClipboardManager.current

    val startingText = initialText?.takeIf { it.isNotBlank() } ?: SAMPLE_TEXT
    var inputText by remember { mutableStateOf(startingText) }
    var editorText by remember { mutableStateOf(startingText) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val languageTag = languageResolver.resolve(
        htmlLanguageTag = null,
        userDefault = options.language.defaultLanguageTag,
        deviceLocaleTag = Locale.getDefault().toLanguageTag(),
    )

    var tokens by remember { mutableStateOf<List<Token>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(isSettingsLoaded, isDarkTheme) {
        if (isSettingsLoaded) {
            repository.ensureInitialColorReset(options.appearance, isDarkTheme)
        }
    }

    fun updateTokensForText(text: String) {
        tokens = buildTokensForText(
            text = text,
            languageTag = languageTag,
            maxWordLength = options.textHandling.maxWordLength,
            tokenizer = tokenizer,
            hyphenator = hyphenator,
        )
        scheduler.load(tokens, options.playback)
        scheduler.restart()
    }

    LaunchedEffect(
        inputText,
        options.textHandling.maxWordLength,
        options.language.defaultLanguageTag,
        isSettingsLoaded,
    ) {
        if (!isSettingsLoaded) return@LaunchedEffect
        if (isUrl(inputText)) {
            statusMessage = "Loading..."
            updateTokensForText(statusMessage ?: "")
            val fetchResult = withContext(Dispatchers.IO) { fetcher.fetch(inputText) }
            when (fetchResult) {
                is FetchResult.Error -> {
                    statusMessage = fetchResult.message
                    updateTokensForText(statusMessage ?: "")
                }
                is FetchResult.Success -> {
                    when (val extractResult = extractor.extract(fetchResult.html)) {
                        is ExtractResult.Error -> {
                            statusMessage = extractResult.message
                            updateTokensForText(statusMessage ?: "")
                        }
                        is ExtractResult.Success -> {
                            statusMessage = null
                            updateTokensForText(extractResult.content.text)
                        }
                    }
                }
            }
        } else {
            statusMessage = null
            updateTokensForText(inputText)
        }
    }

    LaunchedEffect(options.playback, isSettingsLoaded) {
        if (isSettingsLoaded) {
            scheduler.updateOptions(options.playback)
        }
    }

    LaunchedEffect(initialText) {
        val newText = initialText?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        editorText = newText
        inputText = newText
    }

    var clipboardText by remember { mutableStateOf("") }
    LaunchedEffect(clipboardManager) {
        var lastText = ""
        while (isActive) {
            val currentText = clipboardManager.getText()?.text?.toString().orEmpty()
            if (currentText != lastText) {
                clipboardText = currentText
                lastText = currentText
            }
            delay(500)
        }
    }

    var currentIndex by remember { mutableStateOf(0) }
    LaunchedEffect(tokens) {
        scheduler.events.collect { event ->
            currentIndex = event.index
        }
    }

    val current = tokens.getOrNull(currentIndex)
    val next = tokens.getOrNull(currentIndex + 1)
    val canPaste = clipboardText.isNotBlank()
    val buttonContainerColor = Color(options.appearance.buttonBackgroundColor)
    val buttonContentColor = Color(options.appearance.buttonTextColor)
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = buttonContainerColor,
        contentColor = buttonContentColor,
    )
    val iconButtonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = buttonContainerColor,
        contentColor = buttonContentColor,
        disabledContainerColor = buttonContainerColor.copy(alpha = 0.4f),
        disabledContentColor = buttonContentColor.copy(alpha = 0.4f),
    )
    val progress = if (tokens.isNotEmpty()) {
        (currentIndex + 1).coerceIn(0, tokens.size).toFloat() / tokens.size.toFloat()
    } else {
        0f
    }

    val schedulerState by scheduler.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(options.appearance.backgroundColor)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = editorText,
                onValueChange = { editorText = it },
                label = { Text("Input text") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(contrastTextColor(options.appearance.backgroundColor)),
                    unfocusedTextColor = Color(contrastTextColor(options.appearance.backgroundColor)),
                    focusedLabelColor = Color(contrastTextColor(options.appearance.backgroundColor)),
                    unfocusedLabelColor = Color(contrastTextColor(options.appearance.backgroundColor))
                        .copy(alpha = 0.7f),
                    focusedBorderColor = Color(contrastTextColor(options.appearance.backgroundColor)),
                    unfocusedBorderColor = Color(contrastTextColor(options.appearance.backgroundColor))
                        .copy(alpha = 0.5f),
                    cursorColor = Color(contrastTextColor(options.appearance.backgroundColor)),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = {
                    inputText = editorText
                }, colors = buttonColors) {
                    Text("Load Stutter")
                }
                IconButton(
                    enabled = canPaste,
                    onClick = {
                        val currentText = clipboardManager.getText()?.text?.toString().orEmpty()
                        if (currentText.isNotBlank()) {
                            editorText = currentText
                        }
                    },
                    colors = iconButtonColors,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = "Paste from clipboard",
                    )
                }
                FilledIconButton(
                    onClick = {
                        context.startActivity(android.content.Intent(context, MainActivity::class.java))
                    },
                    colors = iconButtonColors,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Open settings",
                    )
                }
            }
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { context -> ReaderView(context) },
                update = { view ->
                    view.setAppearance(options.appearance)
                    view.setShowFlankers(options.textHandling.showFlankers)
                    view.setLanguageTag(languageTag)
                    if (current != null) {
                        view.setWord(current.text, next?.text)
                    } else {
                        view.setWord("", null)
                    }
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .alpha(0.25f)
                    .background(Color(options.appearance.remainderColor)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxSize()
                        .background(Color(options.appearance.centerColor)),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = { scheduler.restart() },
                    modifier = Modifier.size(40.dp),
                    colors = iconButtonColors,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "Restart",
                    )
                }
                Button(onClick = { scheduler.skipBack() }, colors = buttonColors) {
                    Icon(
                        imageVector = Icons.Filled.FastRewind,
                        contentDescription = "Skip back",
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                FloatingActionButton(
                    modifier = Modifier.size(72.dp),
                    onClick = {
                        when (scheduler.state.value) {
                            org.tomasino.stutter.scheduler.SchedulerState.Playing -> scheduler.pause()
                            org.tomasino.stutter.scheduler.SchedulerState.Paused -> scheduler.resume()
                            else -> scheduler.play()
                        }
                    },
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor,
                ) {
                    if (schedulerState == org.tomasino.stutter.scheduler.SchedulerState.Playing) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(48.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { scheduler.skipForward() }, colors = buttonColors) {
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = "Skip forward",
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = buttonContainerColor,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "WPM",
                            color = buttonContentColor,
                        )
                        Text(
                            options.playback.wpm.toString(),
                            color = buttonContentColor,
                        )
                    }
                    Slider(
                        value = options.playback.wpm.toFloat(),
                        onValueChange = { newValue ->
                            scope.launch {
                                repository.setPlaybackOptions(
                                    options.playback.copy(wpm = newValue.toInt())
                                )
                            }
                        },
                        valueRange = PlaybackOptions.MIN_WPM.toFloat()..PlaybackOptions.MAX_WPM.toFloat(),
                        colors = SliderDefaults.colors(
                            activeTrackColor = buttonContentColor,
                            inactiveTrackColor = buttonContentColor.copy(alpha = 0.35f),
                            thumbColor = buttonContentColor,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private const val SAMPLE_TEXT =
    "Welcome to Stutter. " +
    "RSVP, or Rapid Serial Visual Presentation, is a way to read where words appear one by one in the same place. " +
    "This can help your eyes move less, because you are not jumping around the page. " +
    "People use RSVP to read faster or to focus better when long lines are tiring. " +
    "It can also make reading easier when you want a steady, predictable pace. " +
    "Stutter is an RSVP reader that lets you tune timing, language, and appearance, " +
    "including long-word handling across many languages. " +
    "The center button plays and pauses, and the restart button returns to the beginning. " +
    "The left button skips back, and the right button skips forward. " +
    "To reach settings, tap the gear next to Load Stutter. " +
    "In settings, Playback controls timing such as WPM and delays, which lets you match the pace to your comfort and attention. " +
    "Those timing controls exist because different kinds of words and punctuation feel more natural with different pauses. " +
    "A sentence ending can take a longer breath, while short words or numbers can be slightly faster. " +
    "Text handling controls word splitting and flankers so long words do not overflow and the next word can be previewed when that helps. " +
    "Language sets detection and defaults so tokenization, punctuation rules, and hyphenation work correctly for what you are reading. " +
    "Appearance controls font, size, spacing, and colors so the display is comfortable and readable for your eyes. " +
    "Stutter stores no reading history and can be used offline. " +
    "Paste text or a URL above, then tap Load Stutter."

private fun isUrl(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return false
    return trimmed.startsWith("http://") || trimmed.startsWith("https://")
}

private fun contrastTextColor(backgroundColor: Int): Int {
    val red = AndroidColor.red(backgroundColor) / 255.0
    val green = AndroidColor.green(backgroundColor) / 255.0
    val blue = AndroidColor.blue(backgroundColor) / 255.0
    val linearRed = if (red <= 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4)
    val linearGreen = if (green <= 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4)
    val linearBlue = if (blue <= 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4)
    val luminance = 0.2126 * linearRed + 0.7152 * linearGreen + 0.0722 * linearBlue
    return if (luminance > 0.5) AndroidColor.BLACK else AndroidColor.WHITE
}
