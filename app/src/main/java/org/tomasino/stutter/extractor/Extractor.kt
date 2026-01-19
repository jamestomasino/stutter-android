package org.tomasino.stutter.extractor

data class ExtractedContent(
    val text: String,
    val title: String?,
    val languageTag: String?,
)

sealed class ExtractResult {
    data class Success(val content: ExtractedContent) : ExtractResult()
    data class Error(val message: String, val cause: Throwable? = null) : ExtractResult()
}

interface Extractor {
    fun extract(html: String): ExtractResult
}
