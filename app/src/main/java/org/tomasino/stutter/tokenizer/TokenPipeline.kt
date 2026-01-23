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
    val rawTokens = tokenizer.tokenize(text, languageTag)
    return splitLongTokens(
        tokens = rawTokens,
        languageTag = languageTag,
        maxWordLength = maxWordLength,
        hyphenator = hyphenator,
    )
}
