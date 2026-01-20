package org.tomasino.stutter

import android.os.Bundle
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tomasino.stutter.hyphenation.PatternHyphenator
import org.tomasino.stutter.language.BasicLanguageResolver
import org.tomasino.stutter.reader.ReaderView
import org.tomasino.stutter.fetcher.FetchResult
import org.tomasino.stutter.fetcher.OkHttpFetcher
import org.tomasino.stutter.extractor.BasicExtractor
import org.tomasino.stutter.extractor.ExtractResult
import org.tomasino.stutter.settings.SettingsRepository
import org.tomasino.stutter.settings.settingsDataStore
import org.tomasino.stutter.tokenizer.IcuTokenizer
import org.tomasino.stutter.tokenizer.Token
import org.tomasino.stutter.tokenizer.splitLongTokens
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
    val scheduler = remember { org.tomasino.stutter.scheduler.SchedulerImpl(scope) }
    val hyphenator = remember { PatternHyphenator() }
    val tokenizer = remember { IcuTokenizer() }
    val languageResolver = remember { BasicLanguageResolver() }
    val fetcher = remember { OkHttpFetcher() }
    val extractor = remember { BasicExtractor() }

    val startingText = initialText?.takeIf { it.isNotBlank() } ?: SAMPLE_TEXT
    var inputText by remember { mutableStateOf(startingText) }
    var editorText by remember { mutableStateOf(startingText) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val languageTag = languageResolver.resolve(
        htmlLanguageTag = null,
        userDefault = options.language.defaultLanguageTag,
        deviceLocaleTag = Locale.getDefault().toLanguageTag(),
    )

    var tokens by remember { mutableStateOf<List<Token>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(
        inputText,
        options.textHandling.maxWordLength,
        options.language.defaultLanguageTag,
        isSettingsLoaded,
    ) {
        if (!isSettingsLoaded) return@LaunchedEffect
        isLoading = true
        statusMessage = null

        val resolvedText = if (isUrl(inputText)) {
            val fetchResult = withContext(Dispatchers.IO) { fetcher.fetch(inputText) }
            when (fetchResult) {
                is FetchResult.Error -> {
                    statusMessage = fetchResult.message
                    ""
                }
                is FetchResult.Success -> {
                    when (val extractResult = extractor.extract(fetchResult.html)) {
                        is ExtractResult.Error -> {
                            statusMessage = extractResult.message
                            ""
                        }
                        is ExtractResult.Success -> extractResult.content.text
                    }
                }
            }
        } else {
            inputText
        }

        if (resolvedText.isNotBlank()) {
            val rawTokens = tokenizer.tokenize(resolvedText, languageTag)
            tokens = splitLongTokens(
                tokens = rawTokens,
                languageTag = languageTag,
                maxWordLength = options.textHandling.maxWordLength,
                hyphenator = hyphenator,
            )
            scheduler.load(tokens, options.playback)
        } else {
            tokens = emptyList()
        }
        isLoading = false
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

    var currentIndex by remember { mutableStateOf(0) }
    LaunchedEffect(tokens) {
        scheduler.events.collect { event ->
            currentIndex = event.index
        }
    }

    val current = tokens.getOrNull(currentIndex)
    val next = tokens.getOrNull(currentIndex + 1)
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
                label = { Text("Paste text") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = {
                    inputText = editorText
                }) {
                    Text("Load text")
                }
            FilledIconButton(
                onClick = {
                    context.startActivity(android.content.Intent(context, MainActivity::class.java))
                }
            ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Open settings",
                    )
                }
            }
            if (isLoading) {
                Text("Loading...")
            }
            if (!statusMessage.isNullOrEmpty()) {
                Text(statusMessage!!)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { scheduler.skipBack() }) {
                    Icon(
                        imageVector = Icons.Filled.FastRewind,
                        contentDescription = "Skip back",
                    )
                }
                FloatingActionButton(
                    modifier = Modifier.size(72.dp),
                    onClick = {
                        when (scheduler.state.value) {
                            org.tomasino.stutter.scheduler.SchedulerState.Playing -> scheduler.pause()
                            org.tomasino.stutter.scheduler.SchedulerState.Paused -> scheduler.resume()
                            else -> scheduler.play()
                        }
                    },
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
                Button(onClick = { scheduler.skipForward() }) {
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
                FilledIconButton(
                    onClick = { scheduler.restart() },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "Restart",
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
    "To reach settings, tap the gear next to Load text. " +
    "In settings, Playback controls timing such as WPM and delays, which lets you match the pace to your comfort and attention. " +
    "Those timing controls exist because different kinds of words and punctuation feel more natural with different pauses. " +
    "A sentence ending can take a longer breath, while short words or numbers can be slightly faster. " +
    "Text handling controls word splitting and flankers so long words do not overflow and the next word can be previewed when that helps. " +
    "Language sets detection and defaults so tokenization, punctuation rules, and hyphenation work correctly for what you are reading. " +
    "Appearance controls font, size, spacing, and colors so the display is comfortable and readable for your eyes. " +
    "Stutter stores no reading history and can be used offline. " +
    "Paste text or a URL above, then tap Load text."

private fun isUrl(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return false
    return trimmed.startsWith("http://") || trimmed.startsWith("https://")
}
