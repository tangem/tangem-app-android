package com.tangem.core.ui.res

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ButtonColorType(val lightColor: Color, val darkColor: Color) {
    PRIMARY(lightColor = Dark6, darkColor = Light1),
    SECONDARY(lightColor = Light2, darkColor = Dark5),
    DISABLED(lightColor = Light2, darkColor = Dark6),
    POSITIVE(lightColor = Meadow, darkColor = Meadow),
    POSITIVE_DISABLED(lightColor = MagicMint, darkColor = DarkGreen)
}

@Composable
fun Colors.buttonColor(type: ButtonColorType): Color = if (isSystemInDarkTheme) type.darkColor else type.lightColor