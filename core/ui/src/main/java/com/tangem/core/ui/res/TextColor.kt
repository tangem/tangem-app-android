package com.tangem.core.ui.res

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class TextColorType(val lightColor: Color, val darkColor: Color) {
    PRIMARY1(lightColor = Dark6, darkColor = White),
    PRIMARY2(lightColor = White, darkColor = Dark6),
    SECONDARY(lightColor = Dark2, darkColor = Light5),
    TERTIARY(lightColor = Dark1, darkColor = Dark1),
    DISABLED(lightColor = Light4, darkColor = Dark3),
    ACCENT(lightColor = Meadow, darkColor = Meadow),
    WARNING(lightColor = Amaranth, darkColor = Amaranth),
    ATTENTION(lightColor = Tangerine, darkColor = Tangerine),
    CONSTANT_WHITE(lightColor = White, darkColor = White)
}

@Composable
fun Colors.textColor(type: TextColorType): Color = if (isSystemInDarkTheme) type.darkColor else type.lightColor