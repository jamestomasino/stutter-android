package org.tomasino.stutter.hyphenation

import java.util.Locale

class PatternHyphenator(
    private val repository: HyphenationPatternRepository = HyphenationPatternRepository(),
    private val fallback: Hyphenator = FallbackHyphenator(),
) : Hyphenator {
    override fun split(word: String, languageTag: String?, maxLength: Int): List<String> {
        if (maxLength <= 0) return listOf(word)
        if (word.isEmpty()) return listOf(word)

        val (prefix, core, suffix) = splitAffixes(word)
        if (core.isEmpty()) return listOf(word)

        val patternSet = repository.load(languageTag) ?: return fallback.split(word, languageTag, maxLength)
        val coreLength = core.codePointCount(0, core.length)
        if (coreLength <= maxLength) return listOf(word)

        val breakpoints = patternSet.hyphenationPoints(core, languageTag)
        val segments = splitWithBreakpoints(core, breakpoints, maxLength)
        if (segments.isEmpty()) return fallback.split(word, languageTag, maxLength)

        val result = mutableListOf<String>()
        for (i in segments.indices) {
            val segment = segments[i]
            val withPrefix = if (i == 0) prefix + segment else segment
            val withSuffix = if (i == segments.lastIndex) withPrefix + suffix else withPrefix
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

    private fun splitWithBreakpoints(core: String, breakpoints: List<Int>, maxLength: Int): List<String> {
        if (breakpoints.isEmpty()) {
            return splitByCodePoints(core, maxLength)
        }

        val sorted = breakpoints.sorted()
        val segments = mutableListOf<String>()
        var start = 0
        while (start < core.length) {
            val remaining = core.length - start
            val maxEnd = if (remaining <= maxLength) core.length else start + maxLength
            if (maxEnd == core.length) {
                segments.add(core.substring(start))
                break
            }

            val splitAt = sorted.lastOrNull { it > start && it <= maxEnd }
            if (splitAt == null) {
                val fallbackSegments = splitByCodePoints(core.substring(start), maxLength)
                if (fallbackSegments.isEmpty()) {
                    break
                }
                segments.add(fallbackSegments.first())
                start += fallbackSegments.first().length
            } else {
                segments.add(core.substring(start, splitAt))
                start = splitAt
            }
        }
        return segments.filter { it.isNotEmpty() }
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

internal data class HyphenationPatternSet(
    val trie: PatternTrie,
    val minLeft: Int,
    val minRight: Int,
) {
    fun hyphenationPoints(word: String, languageTag: String?): List<Int> {
        if (word.isEmpty()) return emptyList()
        if (word.length <= minLeft + minRight) return emptyList()

        val locale = languageTag
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { Locale.forLanguageTag(it) }
            ?: Locale.getDefault()
        val lower = word.lowercase(locale)
        val work = ".$lower."
        val values = IntArray(work.length + 1)

        for (i in work.indices) {
            var node: PatternTrie? = trie
            var index = i
            while (index < work.length && node != null) {
                node = node.children[work[index]]
                if (node == null) break
                val patternValues = node.values
                if (patternValues != null) {
                    for (offset in patternValues.indices) {
                        val target = i + offset
                        if (target < values.size) {
                            values[target] = maxOf(values[target], patternValues[offset])
                        }
                    }
                }
                index++
            }
        }

        val breakpoints = mutableListOf<Int>()
        for (i in 1 until word.length) {
            val leftSize = i
            val rightSize = word.length - i
            if (leftSize < minLeft || rightSize < minRight) continue
            val value = values[i + 1]
            if (value % 2 == 1) {
                breakpoints.add(i)
            }
        }
        return breakpoints
    }
}

internal class PatternTrie {
    val children: MutableMap<Char, PatternTrie> = mutableMapOf()
    var values: IntArray? = null

    fun insert(pattern: String) {
        if (pattern.isBlank()) return
        val letters = StringBuilder()
        val values = mutableListOf(0)
        for (char in pattern) {
            if (char.isDigit()) {
                values[values.lastIndex] = char.digitToInt()
            } else {
                letters.append(char)
                values.add(0)
            }
        }
        var node = this
        for (char in letters.toString()) {
            node = node.children.getOrPut(char) { PatternTrie() }
        }
        node.values = values.toIntArray()
    }
}
