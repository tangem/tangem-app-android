package com.tangem.core.ui.res

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemColorPalette.Amaranth
import com.tangem.core.ui.res.TangemColorPalette.Dark1
import com.tangem.core.ui.res.TangemColorPalette.Dark2
import com.tangem.core.ui.res.TangemColorPalette.Dark3
import com.tangem.core.ui.res.TangemColorPalette.Dark6
import com.tangem.core.ui.res.TangemColorPalette.Light4
import com.tangem.core.ui.res.TangemColorPalette.Light5
import com.tangem.core.ui.res.TangemColorPalette.Meadow
import com.tangem.core.ui.res.TangemColorPalette.Tangerine
import com.tangem.core.ui.res.TangemColorPalette.White

@Deprecated("Use TangemTheme.colors")
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
fun Colors.textColor(type: TextColorType): Color = if (IS_SYSTEM_IN_DARK_THEME) type.darkColor else type.lightColor
