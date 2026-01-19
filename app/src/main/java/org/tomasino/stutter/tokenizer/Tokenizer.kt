package org.tomasino.stutter.tokenizer

data class Token(
    val text: String,
    val isSentenceEnd: Boolean,
    val isOtherPunctuation: Boolean,
    val isNumeric: Boolean,
    val isShortWord: Boolean,
    val isLongWord: Boolean,
)

interface Tokenizer {
    fun tokenize(text: String, languageTag: String?): List<Token>
}
