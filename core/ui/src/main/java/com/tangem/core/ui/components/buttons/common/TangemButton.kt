package com.tangem.core.ui.components.buttons.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import com.tangem.core.ui.res.TangemTheme

@Suppress("LongParameterList")
@Composable
internal fun TangemButton(
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
        contentPadding = if (showProgress) ButtonDefaults.ContentPadding else size.toContentPadding(icon = icon),
    ) {
        ButtonContent(
            text = text,
            textStyle = textStyle,
            buttonIcon = icon,
            colors = colors,
            showProgress = showProgress,
            enabled = enabled,
            size = size,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ButtonContent(
    text: String,
    textStyle: TextStyle,
    buttonIcon: TangemButtonIconPosition,
    colors: ButtonColors,
    size: TangemButtonSize,
    enabled: Boolean,
    showProgress: Boolean,
) {
    val icon = @Composable { iconResId: Int ->
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size20),
            painter = painterResource(id = iconResId),
            tint = colors.contentColor(enabled = enabled).value,
            contentDescription = null,
        )
    }

    if (showProgress) {
        Box(modifier = Modifier.wrapContentSize()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(TangemTheme.dimens.size24),
                color = colors.contentColor(enabled = enabled).value,
                strokeWidth = TangemTheme.dimens.size4,
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(size.toIconPadding()),
        ) {
            if (buttonIcon is TangemButtonIconPosition.Start) {
                icon(buttonIcon.iconResId)
            }
            Text(
                text = text,
                style = textStyle,
                color = colors.contentColor(enabled = enabled).value,
                maxLines = 1,
            )
            if (buttonIcon is TangemButtonIconPosition.End) {
                icon(buttonIcon.iconResId)
            }
        }
    }
}