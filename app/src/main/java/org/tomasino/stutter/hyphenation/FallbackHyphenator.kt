package org.tomasino.stutter.hyphenation

class FallbackHyphenator : Hyphenator {
    override fun split(word: String, languageTag: String?, maxLength: Int): List<String> {
        if (maxLength <= 0) return listOf(word)
        if (word.isEmpty()) return listOf(word)

        val (prefix, core, suffix) = splitAffixes(word)
        if (core.isEmpty()) return listOf(word)

        val chunks = splitByCodePoints(core, maxLength)
        if (chunks.isEmpty()) return listOf(word)

        val result = mutableListOf<String>()
        for (i in chunks.indices) {
            val chunk = chunks[i]
            val withPrefix = if (i == 0) prefix + chunk else chunk
            val withSuffix = if (i == chunks.lastIndex) withPrefix + suffix else withPrefix
            result.add(withSuffix)
        }
        return result
    }

    private fun splitAffixes(text: String): Triple<String, String, String> {
        var start = 0
        while (start < text.length && !text[start].isLetterOrDigit()) {
            start++
        }
        var end = text.length - 1
        while (end >= start && !text[end].isLetterOrDigit()) {
            end--
        }

        if (start > end) {
            return Triple("", "", "")
        }

        val prefix = text.substring(0, start)
        val core = text.substring(start, end + 1)
        val suffix = text.substring(end + 1)
        return Triple(prefix, core, suffix)
    }

    private fun splitByCodePoints(text: String, maxLength: Int): List<String> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            val remainingCodePoints = text.codePointCount(index, text.length)
            val step = minOf(maxLength, remainingCodePoints)
            val nextIndex = text.offsetByCodePoints(index, step)
            result.add(text.substring(index, nextIndex))
            index = nextIndex
        }
        return result
    }
}
