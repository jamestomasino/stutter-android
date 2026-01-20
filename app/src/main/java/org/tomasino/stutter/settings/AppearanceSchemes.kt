package org.tomasino.stutter.settings

fun solarizedAppearance(base: AppearanceOptions, isDarkTheme: Boolean): AppearanceOptions {
    val scheme = if (isDarkTheme) solarizedDark() else solarizedLight()
    return base.copy(
        backgroundColor = scheme.background,
        leftColor = scheme.left,
        centerColor = scheme.center,
        remainderColor = scheme.remainder,
        flankerColor = scheme.flanker,
    )
}

private data class SolarizedScheme(
    val background: Int,
    val left: Int,
    val center: Int,
    val remainder: Int,
    val flanker: Int,
)

private fun solarizedLight(): SolarizedScheme {
    return SolarizedScheme(
        background = 0xFFFDF6E3.toInt(),
        left = 0xFF586E75.toInt(),
        center = 0xFF268BD2.toInt(),
        remainder = 0xFF657B83.toInt(),
        flanker = 0xFF93A1A1.toInt(),
    )
}

private fun solarizedDark(): SolarizedScheme {
    return SolarizedScheme(
        background = 0xFF002B36.toInt(),
        left = 0xFF839496.toInt(),
        center = 0xFF2AA198.toInt(),
        remainder = 0xFF93A1A1.toInt(),
        flanker = 0xFF586E75.toInt(),
    )
}
