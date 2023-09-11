package com.tangem.core.ui.components.buttons.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.res.TangemTheme

@Suppress("LongParameterList")
@Composable
fun TangemButton(
    text: String,
    icon: TangemButtonIconPosition,
    onClick: () -> Unit,
    colors: ButtonColors,
    showProgress: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    size: TangemButtonSize = TangemButtonSize.Default,
    elevation: ButtonElevation = TangemButtonsDefaults.elevation,
    textStyle: TextStyle = TangemTheme.typography.button,
) {
    Button(
        modifier = modifier.heightIn(min = size.toHeightDp()),
        onClick = { if (!showProgress) onClick() },
        enabled = enabled,
        elevation = elevation,
        shape = size.toShape(),
        colors = colors,
        contentPadding = size.toContentPadding(icon = icon),
    ) {
        ButtonContentContainer(
            buttonIcon = icon,
            iconPadding = size.toIconPadding(),
            showProgress = showProgress,
            progressIndicator = {
                CircularProgressIndicator(
                    modifier = Modifier.buttonContentSize(textStyle),
                    color = colors.contentColor(enabled = enabled).value,
                    strokeWidth = TangemTheme.dimens.size4,
                )
            },
            text = {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = text,
                    style = textStyle,
                    color = colors.contentColor(enabled = enabled).value,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            icon = { iconResId ->
                Icon(
                    modifier = Modifier.buttonContentSize(textStyle),
                    painter = painterResource(id = iconResId),
                    tint = colors.contentColor(enabled = enabled).value,
                    contentDescription = null,
                )
            },
        )
    }
}

@Suppress("LongParameterList")
@Composable
private inline fun RowScope.ButtonContentContainer(
    buttonIcon: TangemButtonIconPosition,
    iconPadding: Dp,
    showProgress: Boolean,
    progressIndicator: @Composable RowScope.() -> Unit,
    text: @Composable RowScope.() -> Unit,
    icon: @Composable RowScope.(Int) -> Unit,
) {
    if (showProgress) {
        progressIndicator()
    } else {
        if (buttonIcon is TangemButtonIconPosition.Start) {
            icon(buttonIcon.iconResId)
            SpacerW(width = iconPadding)
        }
        text()
        if (buttonIcon is TangemButtonIconPosition.End) {
            SpacerW(width = iconPadding)
            icon(buttonIcon.iconResId)
        }
    }
}

private fun Modifier.buttonContentSize(buttonTextStyle: TextStyle): Modifier = composed {
    val minContentElementSize = TangemTheme.dimens.size20
    val maxContentElementSize = remember(key1 = buttonTextStyle.lineHeight) {
        buttonTextStyle.lineHeight.value.dp + 4.dp
    }

    this.requiredSizeIn(
        minWidth = minContentElementSize,
        minHeight = minContentElementSize,
        maxWidth = maxContentElementSize,
        maxHeight = maxContentElementSize,
    )
}