package org.tomasino.stutter.reader

data class WordParts(
    val left: String,
    val center: String,
    val right: String,
)

class WordLayoutCalculator {
    fun split(text: String): WordParts {
        if (text.isEmpty()) return WordParts("", "", "")

        val prefixLength = countLeadingNonWord(text)
        val wordLength = countWordChars(text)
        val baseIndex = computeOrpIndex(wordLength)
        var index = baseIndex
        if (prefixLength > 0) {
            index += prefixLength
        }
        val clampedIndex = index.coerceIn(0, text.length - 1)

        val left = text.substring(0, clampedIndex)
        val center = text.substring(clampedIndex, clampedIndex + 1)
        val right = text.substring(clampedIndex + 1)
        return WordParts(left, center, right)
    }

    private fun computeOrpIndex(wordLength: Int): Int {
        return when {
            wordLength < 2 -> 0
            wordLength < 5 -> 1
            wordLength < 9 -> 2
            wordLength < 14 -> 3
            else -> 4
        }
    }

    private fun countLeadingNonWord(text: String): Int {
        var count = 0
        for (ch in text) {
            if (ch.isLetterOrDigit()) break
            count++
        }
        return count
    }

    private fun countWordChars(text: String): Int {
        var count = 0
        for (ch in text) {
            if (ch.isLetterOrDigit()) count++
        }
        return count
    }
}
