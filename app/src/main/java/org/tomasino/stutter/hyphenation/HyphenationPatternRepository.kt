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
            "en", "en-us", "en_us" -> "hyphenation/hyph-en-us.tex"
            "de", "de-de", "de_1996", "de-1996" -> "hyphenation/hyph-de-1996.tex"
            else -> return null
        }
        val stream = loader(resourcePath) ?: return null
        return stream.use { HyphenationPatternsParser.parse(it) }
    }

    private fun normalizeTag(languageTag: String?): String? {
        val trimmed = languageTag?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return trimmed.replace('_', '-').lowercase(Locale.ROOT)
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
