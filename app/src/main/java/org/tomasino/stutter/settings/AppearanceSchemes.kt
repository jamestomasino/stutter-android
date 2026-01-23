package org.tomasino.stutter.settings

const val COLOR_SCHEME_SOLARIZED = "solarized"
const val COLOR_SCHEME_MONOKAI = "monokai"
const val COLOR_SCHEME_GRUVBOX = "gruvbox"
const val COLOR_SCHEME_DRACULA = "dracula"
const val COLOR_SCHEME_NORD = "nord"
const val COLOR_SCHEME_PAPERCOLOR = "papercolor"
const val DEFAULT_COLOR_SCHEME_ID = COLOR_SCHEME_SOLARIZED

data class ColorSchemeOption(
    val id: String,
    val label: String,
)

val COLOR_SCHEME_OPTIONS = listOf(
    ColorSchemeOption(COLOR_SCHEME_SOLARIZED, "Solarized"),
    ColorSchemeOption(COLOR_SCHEME_MONOKAI, "Monokai"),
    ColorSchemeOption(COLOR_SCHEME_GRUVBOX, "Gruvbox"),
    ColorSchemeOption(COLOR_SCHEME_DRACULA, "Dracula"),
    ColorSchemeOption(COLOR_SCHEME_NORD, "Nord"),
    ColorSchemeOption(COLOR_SCHEME_PAPERCOLOR, "Papercolor"),
)

fun colorSchemeLabel(schemeId: String?): String {
    val resolvedId = schemeId ?: DEFAULT_COLOR_SCHEME_ID
    return COLOR_SCHEME_OPTIONS.firstOrNull { it.id == resolvedId }?.label ?: "Solarized"
}

fun applyColorScheme(
    base: AppearanceOptions,
    schemeId: String?,
    isDarkTheme: Boolean,
): AppearanceOptions {
    val resolvedId = schemeId?.takeIf { id -> COLOR_SCHEME_OPTIONS.any { it.id == id } }
        ?: DEFAULT_COLOR_SCHEME_ID
    val scheme = resolveColorScheme(resolvedId, isDarkTheme)
    return base.copy(
        colorSchemeName = resolvedId,
        backgroundColor = scheme.background,
        leftColor = scheme.left,
        centerColor = scheme.center,
        remainderColor = scheme.remainder,
        flankerColor = scheme.flanker,
        buttonBackgroundColor = scheme.buttonBackground,
        buttonTextColor = scheme.buttonText,
    )
}

private data class AppearanceScheme(
    val background: Int,
    val left: Int,
    val center: Int,
    val remainder: Int,
    val flanker: Int,
    val buttonBackground: Int,
    val buttonText: Int,
)

private fun resolveColorScheme(schemeId: String, isDarkTheme: Boolean): AppearanceScheme {
    return when (schemeId) {
        COLOR_SCHEME_MONOKAI -> monokai()
        COLOR_SCHEME_GRUVBOX -> if (isDarkTheme) gruvboxDark() else gruvboxLight()
        COLOR_SCHEME_DRACULA -> dracula()
        COLOR_SCHEME_NORD -> if (isDarkTheme) nordDark() else nordLight()
        COLOR_SCHEME_PAPERCOLOR -> if (isDarkTheme) papercolorDark() else papercolorLight()
        else -> if (isDarkTheme) solarizedDark() else solarizedLight()
    }
}

private fun solarizedLight(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFFFDF6E3.toInt(),
        left = 0xFF586E75.toInt(),
        center = 0xFF268BD2.toInt(),
        remainder = 0xFF657B83.toInt(),
        flanker = 0xFF93A1A1.toInt(),
        buttonBackground = 0xFFE3DDCC.toInt(),
        buttonText = 0xFF268BD2.toInt(),
    )
}

private fun solarizedDark(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFF002B36.toInt(),
        left = 0xFF839496.toInt(),
        center = 0xFF2AA198.toInt(),
        remainder = 0xFF93A1A1.toInt(),
        flanker = 0xFF586E75.toInt(),
        buttonBackground = 0xFF002730.toInt(),
        buttonText = 0xFF2AA198.toInt(),
    )
}

private fun monokai(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFF272822.toInt(),
        left = 0xFFF8F8F2.toInt(),
        center = 0xFFF92672.toInt(),
        remainder = 0xFFA6E22E.toInt(),
        flanker = 0xFF66D9EF.toInt(),
        buttonBackground = 0xFF23241E.toInt(),
        buttonText = 0xFFF92672.toInt(),
    )
}

private fun gruvboxLight(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFFFBF1C7.toInt(),
        left = 0xFF3C3836.toInt(),
        center = 0xFFAF3A03.toInt(),
        remainder = 0xFF5F5F5F.toInt(),
        flanker = 0xFF7C6F64.toInt(),
        buttonBackground = 0xFFE1D8B3.toInt(),
        buttonText = 0xFFAF3A03.toInt(),
    )
}

private fun gruvboxDark(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFF282828.toInt(),
        left = 0xFFEBDBB2.toInt(),
        center = 0xFFFE8019.toInt(),
        remainder = 0xFFB8BB26.toInt(),
        flanker = 0xFF83A598.toInt(),
        buttonBackground = 0xFF242424.toInt(),
        buttonText = 0xFFFE8019.toInt(),
    )
}

private fun dracula(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFF282A36.toInt(),
        left = 0xFFF8F8F2.toInt(),
        center = 0xFFFF79C6.toInt(),
        remainder = 0xFFBD93F9.toInt(),
        flanker = 0xFF8BE9FD.toInt(),
        buttonBackground = 0xFF242530.toInt(),
        buttonText = 0xFFBD93F9.toInt(),
    )
}

private fun nordLight(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFFECEFF4.toInt(),
        left = 0xFF2E3440.toInt(),
        center = 0xFF5E81AC.toInt(),
        remainder = 0xFF4C566A.toInt(),
        flanker = 0xFF81A1C1.toInt(),
        buttonBackground = 0xFFD4D7DC.toInt(),
        buttonText = 0xFF5E81AC.toInt(),
    )
}

private fun nordDark(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFF2E3440.toInt(),
        left = 0xFFD8DEE9.toInt(),
        center = 0xFF88C0D0.toInt(),
        remainder = 0xFFE5E9F0.toInt(),
        flanker = 0xFF81A1C1.toInt(),
        buttonBackground = 0xFF292E39.toInt(),
        buttonText = 0xFF88C0D0.toInt(),
    )
}

private fun papercolorLight(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFFEEEEEE.toInt(),
        left = 0xFF444444.toInt(),
        center = 0xFF005F87.toInt(),
        remainder = 0xFF585858.toInt(),
        flanker = 0xFF878787.toInt(),
        buttonBackground = 0xFFD6D6D6.toInt(),
        buttonText = 0xFF005F87.toInt(),
    )
}

private fun papercolorDark(): AppearanceScheme {
    return AppearanceScheme(
        background = 0xFF1C1C1C.toInt(),
        left = 0xFFD0D0D0.toInt(),
        center = 0xFF5FAFD7.toInt(),
        remainder = 0xFFAFAFAF.toInt(),
        flanker = 0xFF808080.toInt(),
        buttonBackground = 0xFF191919.toInt(),
        buttonText = 0xFF5FAFD7.toInt(),
    )
}
