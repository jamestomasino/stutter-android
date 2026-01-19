package org.tomasino.stutter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tomasino.stutter.hyphenation.FallbackHyphenator
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialText = extractText(intent)
        setContent {
            StutterAndroidTheme {
                ReaderScreen(settingsRepository, initialText)
            }
        }
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
    val scope = rememberCoroutineScope()
    val scheduler = remember { org.tomasino.stutter.scheduler.SchedulerImpl(scope) }
    val hyphenator = remember { FallbackHyphenator() }
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

    LaunchedEffect(inputText, options.textHandling.maxWordLength, options.language.defaultLanguageTag) {
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

    var currentIndex by remember { mutableStateOf(0) }
    LaunchedEffect(tokens) {
        scheduler.events.collect { event ->
            currentIndex = event.index
        }
    }

    val current = tokens.getOrNull(currentIndex)
    val next = tokens.getOrNull(currentIndex + 1)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = editorText,
            onValueChange = { editorText = it },
            label = { Text("Paste text") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 6,
        )
        Button(onClick = {
            inputText = editorText
        }) {
            Text("Load text")
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
                if (current != null) {
                    view.setWord(current.text, next?.text)
                }
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scheduler.play() }) { Text("Play") }
            Button(onClick = { scheduler.pause() }) { Text("Pause") }
            Button(onClick = { scheduler.resume() }) { Text("Resume") }
            Button(onClick = { scheduler.restart() }) { Text("Restart") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scheduler.skipBack() }) { Text("Back") }
            Button(onClick = { scheduler.skipForward() }) { Text("Forward") }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private const val SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog. " +
    "This is sample text for the reader view."

private fun isUrl(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return false
    return trimmed.startsWith("http://") || trimmed.startsWith("https://")
}
