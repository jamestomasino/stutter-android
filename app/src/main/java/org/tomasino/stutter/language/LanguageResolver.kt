package org.tomasino.stutter.language

interface LanguageResolver {
    fun resolve(htmlLanguageTag: String?, userDefault: String?, deviceLocaleTag: String?): String?
}
