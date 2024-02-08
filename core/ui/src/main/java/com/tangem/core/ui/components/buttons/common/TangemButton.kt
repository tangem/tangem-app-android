package com.tangem.core.ui.components.buttons.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.MultipleClickPreventer

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
    additionalText: String? = null,
    size: TangemButtonSize = TangemButtonSize.Default,
    elevation: ButtonElevation = TangemButtonsDefaults.elevation,
    textStyle: TextStyle = TangemTheme.typography.button,
    shape: Shape = size.toShape(),
) {
    val multipleClickPreventer = remember { MultipleClickPreventer.get() }

    Button(
        modifier = modifier.heightIn(min = size.toHeightDp()),
        onClick = {
            multipleClickPreventer.processEvent { if (!showProgress) onClick() }
        },
        enabled = enabled,
        elevation = elevation,
        shape = shape,
        colors = colors,
        contentPadding = size.toContentPadding(icon = icon),
    ) {
        val maxContentSize = getMaxButtonContentSize(buttonTextStyle = textStyle)

        ButtonContentContainer(
            buttonIcon = icon,
            iconPadding = size.toIconPadding(),
            showProgress = showProgress,
            progressIndicator = {
                CircularProgressIndicator(
                    modifier = Modifier.buttonContentSize(maxContentSize),
                    color = colors.contentColor(enabled = enabled).value,
                )
            },
            text = {
                ResizableText(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(MinButtonContentSize, maxContentSize),
                    text = text,
                    style = textStyle,
                    color = colors.contentColor(enabled = enabled).value,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    minFontSize = 12.sp,
                )
            },
            icon = { iconResId ->
                Icon(
                    modifier = Modifier.buttonContentSize(maxContentSize),
                    painter = painterResource(id = iconResId),
                    tint = colors.contentColor(enabled = enabled).value,
                    contentDescription = null,
                )
            },
            additionalText = {
                if (additionalText != null) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = additionalText,
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.disabled,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
    additionalText: @Composable () -> Unit,
) {
    if (showProgress) {
        progressIndicator()
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.Center) {
                if (buttonIcon is TangemButtonIconPosition.Start) {
                    icon(buttonIcon.iconResId)
                    Spacer(modifier = Modifier.requiredWidth(iconPadding))
                }
                text()
                if (buttonIcon is TangemButtonIconPosition.End) {
                    Spacer(modifier = Modifier.requiredWidth(iconPadding))
                    icon(buttonIcon.iconResId)
                }
            }
            additionalText()
        }
    }
}

private val MinButtonContentSize: Dp
    @Composable
    @ReadOnlyComposable
    get() = TangemTheme.dimens.size20

@Composable
@ReadOnlyComposable
private fun getMaxButtonContentSize(buttonTextStyle: TextStyle): Dp {
    val buttonLineHeight = with(LocalDensity.current) {
        buttonTextStyle.lineHeight.toDp()
    }

    return buttonLineHeight.coerceAtLeast(MinButtonContentSize)
}

private fun Modifier.buttonContentSize(maxSize: Dp): Modifier = composed {
    this.requiredSizeIn(
        minWidth = MinButtonContentSize,
        minHeight = MinButtonContentSize,
        maxWidth = maxSize,
        maxHeight = maxSize,
    )
}