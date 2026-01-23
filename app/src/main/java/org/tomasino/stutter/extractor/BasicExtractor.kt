package org.tomasino.stutter.extractor

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class BasicExtractor : Extractor {
    override fun extract(html: String): ExtractResult {
        if (html.isBlank()) return ExtractResult.Error("Empty HTML")
        val document = Jsoup.parse(html)
        val languageTag = document.selectFirst("html")?.attr("lang")?.trim().takeUnless { it.isNullOrEmpty() }

        removeNoise(document)

        val contentText = extractMainText(document)
        if (contentText.isBlank()) {
            return ExtractResult.Error("No readable content")
        }

        val title = document.title().ifBlank { null }
        return ExtractResult.Success(
            ExtractedContent(
                text = contentText,
                title = title,
                languageTag = languageTag,
            )
        )
    }

    private fun removeNoise(document: Document) {
        document.select("script,style,noscript,iframe").remove()
    }

    private fun extractMainText(document: Document): String {
        val article = document.selectFirst("article")
        if (article != null) {
            return article.text()
        }

        val candidates = document.select("main,section,div")
        val best = candidates.maxByOrNull { element ->
            element.select("p").joinToString(" ") { it.text() }.length
        }

        val bestText = best?.select("p")?.joinToString(" ") { it.text() }?.trim().orEmpty()
        if (bestText.isNotEmpty()) return bestText

        val paragraphText = document.select("p").joinToString(" ") { it.text() }.trim()
        if (paragraphText.isNotEmpty()) return paragraphText

        return document.body()?.text()?.trim().orEmpty()
    }

}
