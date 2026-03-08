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
            val articleText = extractStructuredText(article)
            if (articleText.isNotEmpty()) return articleText
        }

        val candidates = document.select("main,section,div")
        val best = candidates.maxByOrNull { element ->
            extractStructuredText(element).length
        }

        val bestText = best?.let(::extractStructuredText).orEmpty()
        if (bestText.isNotEmpty()) return bestText

        val paragraphText = document.select("p")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
            .trim()
        if (paragraphText.isNotEmpty()) return paragraphText

        return document.body().text().trim()
    }

    private fun extractStructuredText(root: Element): String {
        val blocks = root.select("p,blockquote,li,h1,h2,h3,h4,h5,h6,pre")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
        if (blocks.isNotEmpty()) {
            return blocks.joinToString("\n\n")
        }
        return root.text().trim()
    }
}
