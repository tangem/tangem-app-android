package com.tangem.core.ui.res

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class IconColorType(val lightColor: Color, val darkColor: Color) {
    PRIMARY1(lightColor = Black, darkColor = White),
    PRIMARY2(lightColor = White, darkColor = Dark6),
    SECONDARY(lightColor = Dark2, darkColor = Dark1),
    INFORMATIVE(lightColor = Light5, darkColor = Dark2),
    INACTIVE(lightColor = Light4, darkColor = Dark4),
    ACCENT(lightColor = Meadow, darkColor = Meadow),
    WARNING(lightColor = Amaranth, darkColor = Amaranth),
    ATTENTION(lightColor = Tangerine, darkColor = Tangerine)
}

@Composable
fun Colors.iconColor(type: IconColorType): Color = if (isSystemInDarkTheme) type.darkColor else type.lightColor