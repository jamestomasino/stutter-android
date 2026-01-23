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

        val hyphenSplit = splitAtHyphenBoundaries(
            text = token.text,
            maxWordLength = maxWordLength,
        )
        if (hyphenSplit != null) {
            hyphenSplit.forEach { segment ->
                if (classifier.wordLength(segment, languageTag) <= maxWordLength) {
                    result.add(classifier.classify(segment, languageTag))
                } else {
                    result.addAll(
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
            continue
        }

        result.addAll(
            splitTokenByHyphenation(
                text = token.text,
                languageTag = languageTag,
                maxWordLength = maxWordLength,
                hyphenator = hyphenator,
                classifier = classifier,
            )
        )
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
    maxWordLength: Int,
): List<String>? {
    if (!text.any { HYPHEN_CHARS.contains(it) }) return null

    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    var wordCharCount = 0
    var i = 0
    while (i < text.length) {
        val current = text[i]
        if (HYPHEN_CHARS.contains(current)) {
            val nextWordLen = nextWordRunLength(text, i + 1)
            if (nextWordLen > 0) {
                val prospectiveLen = wordCharCount + nextWordLen
                if (prospectiveLen > maxWordLength) {
                    buffer.append(current)
                    if (buffer.isNotEmpty()) {
                        result.add(buffer.toString())
                    }
                    buffer.setLength(0)
                    wordCharCount = 0
                    i++
                    continue
                }
            }
        }

        buffer.append(current)
        if (current.isLetterOrDigit()) {
            wordCharCount++
        }
        i++
    }
    if (buffer.isNotEmpty()) {
        result.add(buffer.toString())
    }

    return if (result.size > 1) result else null
}

private fun nextWordRunLength(text: String, startIndex: Int): Int {
    if (startIndex >= text.length) return 0
    if (!text[startIndex].isLetterOrDigit()) return 0
    var i = startIndex
    while (i < text.length && text[i].isLetterOrDigit()) {
        i++
    }
    return i - startIndex
}

private fun appendHyphen(segment: String): String {
    if (segment.isEmpty()) return segment
    val last = segment.last()
    if (HYPHEN_CHARS.contains(last)) return segment
    return if (last.isLetterOrDigit()) "$segment-" else segment
}

private val HYPHEN_CHARS = setOf('-', '\u2010', '\u2011', '\u2012', '\u2013', '\u2212')
