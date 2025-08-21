package com.tangem.features.home.impl.ui.compose.views

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun StoriesButton(
    text: String,
    useDarkerColors: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: TangemButtonIconPosition = TangemButtonIconPosition.None,
    showProgress: Boolean = false,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = icon,
        colors = if (useDarkerColors) DarkerButtonColors else LighterButtonColors,
        showProgress = showProgress,
        enabled = true,
        shape = TangemTheme.shapes.roundedCornersXMedium,
        textStyle = TangemTheme.typography.subtitle1,
        iconPadding = when (icon) {
            is TangemButtonIconPosition.Start -> TangemTheme.dimens.spacing4
            is TangemButtonIconPosition.End,
            is TangemButtonIconPosition.None,
            -> TangemTheme.dimens.spacing8
        },
        onClick = onClick,
    )
}

private val LighterButtonColors: ButtonColors = ButtonColors(
    containerColor = TangemColorPalette.Light4,
    contentColor = TangemColorPalette.Dark6,
    disabledContainerColor = TangemColorPalette.Dark5,
    disabledContentColor = TangemColorPalette.Dark6,
)

private val DarkerButtonColors: ButtonColors = ButtonColors(
    containerColor = TangemColorPalette.Dark4,
    contentColor = TangemColorPalette.White,
    disabledContainerColor = TangemColorPalette.Dark4,
    disabledContentColor = TangemColorPalette.White,
)