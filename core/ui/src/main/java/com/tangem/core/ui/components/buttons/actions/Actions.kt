package com.tangem.core.ui.components.buttons.actions

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.icons.badge.drawBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TokenDetailsScreenTestTags

/**
 * Rounded action button
 *
 * @param config   component config
 * @param modifier modifier
 * @param color    background color
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=290-306&mode=design&t=
 * cSLQtIbR2J1h5M9U-4">Show in Figma</a>
 */
@Composable
fun RoundedActionButton(
    config: ActionButtonConfig,
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors.button.secondary,
) {
    ActionBaseButton(
        config = config,
        shape = RoundedCornerShape(size = TangemTheme.dimens.radius24),
        content = {
            ActionButtonContent(
                config = config,
                text = { Text(text = config.text, textColor = it) },
                modifier = it.padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing24,
                ),
            )
        },
        modifier = modifier,
        color = color,
    )
}

/**
 * Action button
 *
 * @param config   component config
 * @param modifier modifier
 * @param color    background color
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=290-306&mode=design&t=
 * cSLQtIbR2J1h5M9U-4">Show in Figma</a>
 */
@Composable
fun ActionButton(
    config: ActionButtonConfig,
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors.button.secondary,
    containerColor: Color = TangemTheme.colors.background.secondary,
) {
    ActionBaseButton(
        config = config,
        shape = RoundedCornerShape(size = TangemTheme.dimens.radius12),
        content = {
            ActionButtonContent(
                config = config,
                text = { textColor -> Text(text = config.text, textColor = textColor) },
                modifier = it.padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing24,
                ),
            )
        },
        modifier = modifier.testTag(TokenDetailsScreenTestTags.ACTION_BUTTON),
        color = color,
        containerColor = containerColor,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionBaseButton(
    config: ActionButtonConfig,
    shape: RoundedCornerShape,
    content: @Composable (modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors.button.secondary,
    containerColor: Color = TangemTheme.colors.background.secondary,
) {
    val context = LocalContext.current
    val backgroundColor by animateColorAsState(
        targetValue = if (config.enabled) color else TangemTheme.colors.button.disabled,
        label = "Update background color",
    )

    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .widthIn(min = 100.dp)
            .drawWithContent {
                drawContent()
                if (config.showBadge) {
                    drawBadge(containerColor = containerColor)
                }
            }
            .clip(shape)
            .combinedClickable(
                enabled = config.enabled,
                onClick = config.onClick,
                onLongClick = {
                    val toastReference = config.onLongClick?.invoke()
                    toastReference?.let {
                        Toast
                            .makeText(context, toastReference.resolveReference(context.resources), Toast.LENGTH_SHORT)
                            .show()
                    }
                },
            )
            .background(color = backgroundColor),
    ) {
        content(Modifier.align(Alignment.Center))

        if (config.isInProgress) {
            Loading(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .matchParentSize(),
                backgroundColor = backgroundColor,
            )
        }
    }
}

@Composable
fun ActionButtonContent(
    config: ActionButtonConfig,
    text: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
    paddingBetweenIconAndText: Dp = 8.dp,
) {
    Row(
        modifier = modifier
            .padding(vertical = TangemTheme.dimens.spacing8),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconTint = when {
            !config.enabled -> TangemTheme.colors.icon.informative
            config.dimContent -> TangemTheme.colors.icon.informative
            else -> TangemTheme.colors.icon.primary1
        }
        Icon(
            modifier = Modifier.size(size = TangemTheme.dimens.size20),
            painter = painterResource(id = config.iconResId),
            contentDescription = null,
            tint = iconTint,
        )

        Spacer(modifier = Modifier.width(width = paddingBetweenIconAndText))

        text(getTextColor(config))
    }
}

@Composable
private fun Text(text: TextReference, textColor: Color) {
    Text(
        text = text.resolveReference(),
        color = textColor,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TangemTheme.typography.button,
    )
}

@Composable
private fun Loading(backgroundColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(color = backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            color = TangemTheme.colors.icon.accent,
        )
    }
}

@Composable
@ReadOnlyComposable
fun getTextColor(config: ActionButtonConfig): Color {
    return when {
        !config.enabled -> TangemTheme.colors.text.disabled
        config.dimContent -> TangemTheme.colors.text.tertiary
        else -> TangemTheme.colors.text.primary1
    }
}

@Preview(group = "RoundedActionButton", showBackground = true)
@Preview(group = "RoundedActionButton", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_RoundedActionButton(@PreviewParameter(ActionStateProvider::class) state: ActionButtonConfig) {
    TangemThemePreview {
        RoundedActionButton(state)
    }
}

@Preview(group = "ActionButton", showBackground = true)
@Preview(group = "ActionButton", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ActionButton(@PreviewParameter(ActionStateProvider::class) state: ActionButtonConfig) {
    TangemThemePreview {
        ActionButton(state)
    }
}

private class ActionStateProvider : CollectionPreviewParameterProvider<ActionButtonConfig>(
    collection = listOf(
        ActionButtonConfig(
            text = TextReference.Str(value = "Enabled"),
            iconResId = R.drawable.ic_arrow_up_24,
            enabled = true,
            onClick = {},
            showBadge = true,
        ),
        ActionButtonConfig(
            text = TextReference.Str(value = "Dimmed"),
            iconResId = R.drawable.ic_arrow_up_24,
            enabled = true,
            dimContent = true,
            onClick = {},
        ),
        ActionButtonConfig(
            text = TextReference.Str(value = "Disabled"),
            iconResId = R.drawable.ic_arrow_down_24,
            enabled = false,
            onClick = {},
        ),
        ActionButtonConfig(
            text = TextReference.Str(value = "Loading"),
            iconResId = R.drawable.ic_arrow_down_24,
            enabled = false,
            onClick = {},
            isInProgress = true,
        ),
    ),
)