package com.tangem.core.ui.res

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemColorPalette.Dark5
import com.tangem.core.ui.res.TangemColorPalette.Dark6
import com.tangem.core.ui.res.TangemColorPalette.DarkGreen
import com.tangem.core.ui.res.TangemColorPalette.Light1
import com.tangem.core.ui.res.TangemColorPalette.Light2
import com.tangem.core.ui.res.TangemColorPalette.MagicMint
import com.tangem.core.ui.res.TangemColorPalette.Meadow

@Deprecated("Use TangemTheme.colors")
enum class ButtonColorType(val lightColor: Color, val darkColor: Color) {
    PRIMARY(lightColor = Dark6, darkColor = Light1),
    SECONDARY(lightColor = Light2, darkColor = Dark5),
    DISABLED(lightColor = Light2, darkColor = Dark6),
    POSITIVE(lightColor = Meadow, darkColor = Meadow),
    POSITIVE_DISABLED(lightColor = MagicMint, darkColor = DarkGreen)
}

@Composable
fun Colors.buttonColor(type: ButtonColorType): Color = if (IS_SYSTEM_IN_DARK_THEME) type.darkColor else type.lightColor
