package org.tomasino.stutter.language

import java.util.Locale

class BasicLanguageResolver : LanguageResolver {
    override fun resolve(
        htmlLanguageTag: String?,
        userDefault: String?,
        deviceLocaleTag: String?,
    ): String? {
        return listOf(htmlLanguageTag, userDefault, deviceLocaleTag)
            .asSequence()
            .map { normalizeTag(it) }
            .firstOrNull { it != null }
    }

    private fun normalizeTag(tag: String?): String? {
        val trimmed = tag?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val locale = Locale.forLanguageTag(trimmed)
        val language = locale.language
        if (language.isNullOrEmpty() || language == "und") {
            return null
        }
        return locale.toLanguageTag()
    }
}
