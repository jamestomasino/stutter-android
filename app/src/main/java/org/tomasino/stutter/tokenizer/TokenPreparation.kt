package org.tomasino.stutter.tokenizer

import org.tomasino.stutter.hyphenation.Hyphenator

fun splitLongTokens(
    tokens: List<Token>,
    languageTag: String?,
    maxWordLength: Int,
    hyphenator: Hyphenator,
    classifier: TokenClassifier = TokenClassifier(),
): List<Token> {
    if (tokens.isEmpty()) return emptyList()
    if (maxWordLength <= 0) return tokens

    val result = mutableListOf<Token>()
    for (token in tokens) {
        val wordLength = classifier.wordLength(token.text, languageTag)
        if (wordLength <= maxWordLength) {
            result.add(token)
            continue
        }

        val segments = hyphenator.split(token.text, languageTag, maxWordLength)
        if (segments.isEmpty()) {
            result.add(token)
            continue
        }

        val cleaned = segments.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) {
            result.add(token)
            continue
        }

        cleaned.mapTo(result) { segment ->
            classifier.classify(segment, languageTag)
        }
    }
    return result
}
