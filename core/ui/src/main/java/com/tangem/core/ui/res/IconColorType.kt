package com.tangem.core.ui.res

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemColorPalette.Amaranth
import com.tangem.core.ui.res.TangemColorPalette.Black
import com.tangem.core.ui.res.TangemColorPalette.Dark1
import com.tangem.core.ui.res.TangemColorPalette.Dark2
import com.tangem.core.ui.res.TangemColorPalette.Dark4
import com.tangem.core.ui.res.TangemColorPalette.Dark6
import com.tangem.core.ui.res.TangemColorPalette.Light4
import com.tangem.core.ui.res.TangemColorPalette.Light5
import com.tangem.core.ui.res.TangemColorPalette.Meadow
import com.tangem.core.ui.res.TangemColorPalette.Tangerine
import com.tangem.core.ui.res.TangemColorPalette.White

@Deprecated("Use TangemTheme.colors")
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
fun Colors.iconColor(type: IconColorType): Color = if (IS_SYSTEM_IN_DARK_THEME) type.darkColor else type.lightColor
