package org.tomasino.stutter.hyphenation

import java.io.InputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class HyphenationPatternRepository(
    private val loader: (String) -> InputStream? = { path ->
        val threadLoader = Thread.currentThread().contextClassLoader
        val classLoader = threadLoader ?: HyphenationPatternRepository::class.java.classLoader
        classLoader?.getResourceAsStream(path)
    },
) {
    private val cache = ConcurrentHashMap<String, HyphenationPatternSet?>()

    internal fun load(languageTag: String?): HyphenationPatternSet? {
        val key = normalizeTag(languageTag) ?: return null
        return cache.computeIfAbsent(key) { loadFromResources(it) }
    }

    private fun loadFromResources(normalizedTag: String): HyphenationPatternSet? {
        val resourcePath = when (normalizedTag) {
            "ar" -> "hyphenation/hyph-ar.tex"
            "bn" -> "hyphenation/hyph-bn.tex"
            "de" -> "hyphenation/hyph-de-1996.tex"
            "en" -> "hyphenation/hyph-en-us.tex"
            "es" -> "hyphenation/hyph-es.tex"
            "fa" -> "hyphenation/hyph-fa.tex"
            "fr" -> "hyphenation/hyph-fr.tex"
            "hi" -> "hyphenation/hyph-hi.tex"
            "id" -> "hyphenation/hyph-id.tex"
            "it" -> "hyphenation/hyph-it.tex"
            "mr" -> "hyphenation/hyph-mr.tex"
            "pt" -> "hyphenation/hyph-pt.tex"
            "ru" -> "hyphenation/hyph-ru.tex"
            "ta" -> "hyphenation/hyph-ta.tex"
            "te" -> "hyphenation/hyph-te.tex"
            "th" -> "hyphenation/hyph-th.tex"
            "tr" -> "hyphenation/hyph-tr.tex"
            "vi" -> "hyphenation/hyph-vi.tex"
            else -> return null
        }
        val stream = loader(resourcePath) ?: return null
        return stream.use { HyphenationPatternsParser.parse(it) }
    }

    private fun normalizeTag(languageTag: String?): String? {
        val trimmed = languageTag?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalized = trimmed.replace('_', '-')
        val locale = Locale.forLanguageTag(normalized)
        val language = locale.language.takeIf { it.isNotEmpty() && it != "und" } ?: return null
        return language.lowercase(Locale.ROOT)
    }
}

internal object HyphenationPatternsParser {
    fun parse(input: InputStream): HyphenationPatternSet {
        val text = input.bufferedReader().use { it.readText() }
        val minLeft = parseHyphenMin(text, "left") ?: 2
        val minRight = parseHyphenMin(text, "right") ?: 2
        val trie = PatternTrie()
        val cleaned = text
            .lineSequence()
            .map { it.substringBefore('%') }
            .joinToString("\n")
            .replace("\\patterns", " ")
            .replace("\\hyphenation", " ")
            .replace("{", " ")
            .replace("}", " ")
        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        for (token in tokens) {
            if (token.startsWith("\\")) continue
            if (!token.all { it.isLetterOrDigit() || it == '.' }) continue
            trie.insert(token)
        }
        return HyphenationPatternSet(trie, minLeft, minRight)
    }

    private fun parseHyphenMin(text: String, key: String): Int? {
        val regex = Regex("%\\s*$key:\\s*(\\d+)", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
}
