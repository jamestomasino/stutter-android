package org.tomasino.stutter.hyphenation

interface Hyphenator {
    fun split(word: String, languageTag: String?, maxLength: Int): List<String>
}
