package org.tomasino.stutter.tokenizer

import org.tomasino.stutter.hyphenation.Hyphenator

fun buildTokensForText(
    text: String,
    languageTag: String?,
    maxWordLength: Int,
    tokenizer: Tokenizer,
    hyphenator: Hyphenator,
): List<Token> {
    if (text.isBlank()) return emptyList()
    val paragraphs = splitParagraphs(text)
    if (paragraphs.isEmpty()) return emptyList()

    val combined = mutableListOf<Token>()
    paragraphs.forEach { paragraph ->
        val rawTokens = tokenizer.tokenize(paragraph, languageTag)
        if (rawTokens.isEmpty()) return@forEach
        val splitTokens = splitLongTokens(
            tokens = rawTokens,
            languageTag = languageTag,
            maxWordLength = maxWordLength,
            hyphenator = hyphenator,
        )
        if (splitTokens.isEmpty()) return@forEach
        splitTokens.forEachIndexed { index, token ->
            combined.add(token.copy(isParagraphEnd = index == splitTokens.lastIndex))
        }
    }
    return combined
}

private fun splitParagraphs(text: String): List<String> {
    return text
        .split(PARAGRAPH_SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private val PARAGRAPH_SEPARATOR = Regex("""(?:\r?\n)[ \t\f]*\r?\n+""")
