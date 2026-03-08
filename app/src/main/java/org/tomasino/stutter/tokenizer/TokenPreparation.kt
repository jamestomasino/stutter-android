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
        val hyphenSplit = splitAtHyphenBoundaries(
            text = token.text,
        )
        if (hyphenSplit != null) {
            val splitTokens = mutableListOf<Token>()
            hyphenSplit.forEach { segment ->
                if (classifier.wordLength(segment, languageTag) <= maxWordLength) {
                    splitTokens.add(classifier.classify(segment, languageTag))
                } else {
                    splitTokens.addAll(
                        splitTokenByHyphenation(
                            text = segment,
                            languageTag = languageTag,
                            maxWordLength = maxWordLength,
                            hyphenator = hyphenator,
                            classifier = classifier,
                        )
                    )
                }
            }
            result.addAll(applyParagraphBoundary(splitTokens, token.isParagraphEnd))
            continue
        }

        val wordLength = classifier.wordLength(token.text, languageTag)
        if (wordLength <= maxWordLength) {
            result.add(token)
            continue
        }

        val splitTokens = splitTokenByHyphenation(
            text = token.text,
            languageTag = languageTag,
            maxWordLength = maxWordLength,
            hyphenator = hyphenator,
            classifier = classifier,
        )
        result.addAll(applyParagraphBoundary(splitTokens, token.isParagraphEnd))
    }
    return result
}

private fun splitTokenByHyphenation(
    text: String,
    languageTag: String?,
    maxWordLength: Int,
    hyphenator: Hyphenator,
    classifier: TokenClassifier,
): List<Token> {
    val segments = hyphenator.split(text, languageTag, maxWordLength)
    if (segments.isEmpty()) {
        return listOf(classifier.classify(text, languageTag))
    }

    val cleaned = segments.filter { it.isNotEmpty() }
    if (cleaned.isEmpty()) {
        return listOf(classifier.classify(text, languageTag))
    }

    val lastIndex = cleaned.lastIndex
    return cleaned.mapIndexed { index, segment ->
        val tokenText = if (index < lastIndex) appendHyphen(segment) else segment
        classifier.classify(tokenText, languageTag)
    }
}

private fun splitAtHyphenBoundaries(
    text: String,
): List<String>? {
    if (!text.any { HYPHEN_CHARS.contains(it) }) return null

    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    for (current in text) {
        buffer.append(current)
        if (HYPHEN_CHARS.contains(current)) {
            if (buffer.isNotEmpty()) {
                result.add(buffer.toString())
            }
            buffer.setLength(0)
        }
    }
    if (buffer.isNotEmpty()) {
        result.add(buffer.toString())
    }

    return if (result.size > 1) result else null
}

private fun appendHyphen(segment: String): String {
    if (segment.isEmpty()) return segment
    val last = segment.last()
    if (HYPHEN_CHARS.contains(last)) return segment
    return if (last.isLetterOrDigit()) "$segment-" else segment
}

private fun applyParagraphBoundary(tokens: List<Token>, isParagraphEnd: Boolean): List<Token> {
    if (!isParagraphEnd || tokens.isEmpty()) return tokens
    return tokens.mapIndexed { index, token ->
        if (index == tokens.lastIndex) token.copy(isParagraphEnd = true)
        else token.copy(isParagraphEnd = false)
    }
}

private val HYPHEN_CHARS = setOf(
    '-',
    '\u2010', // hyphen
    '\u2011', // non-breaking hyphen
    '\u2012', // figure dash
    '\u2013', // en dash
    '\u2014', // em dash
    '\u2015', // horizontal bar
    '\u2212', // minus sign
    '\u2043', // hyphen bullet
    '\uFE63', // small hyphen-minus
    '\uFF0D', // fullwidth hyphen-minus
)
