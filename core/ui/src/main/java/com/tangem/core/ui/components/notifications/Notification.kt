package com.tangem.core.ui.components.notifications

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.components.notifications.NotificationConfig.ButtonsState as NotificationButtonsState

/**
 * Notification component from Design system.
 * Use this for Notification with title, subtitle, clickable or not.
 *
 * @param config   component config
 * @param modifier modifier
 * @param iconTint icon tint
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1045-807&t=6CVvYDJe0sB7wBKE-0"
 * >Figma component</a>
 */
@Composable
fun Notification(
    config: NotificationConfig,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    iconTint: Color? = null,
) {
    BaseContainer(
        buttonsState = config.buttonsState,
        onClick = config.onClick,
        modifier = modifier,
        containerColor = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12),
        ) {
            MainContent(
                iconResId = config.iconResId,
                iconTint = iconTint,
                title = config.title,
                subtitle = config.subtitle,
                isClickableComponent = config.onClick != null,
            )

            Buttons(state = config.buttonsState)
        }

        CloseableIconButton(
            onClick = config.onCloseClick,
            modifier = Modifier.align(alignment = Alignment.TopEnd),
        )
    }
}

@Composable
private fun BaseContainer(
    buttonsState: NotificationConfig.ButtonsState?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val tempContainerColor by rememberUpdatedState(
        newValue = if (buttonsState != null || onClick != null) {
            TangemTheme.colors.background.primary
        } else {
            TangemTheme.colors.button.disabled
        },
    )

    Surface(
        onClick = onClick ?: {},
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size62)
            .fillMaxWidth(),
        enabled = onClick != null,
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = containerColor ?: tempContainerColor,
    ) {
        Box(content = content)
    }
}

@Composable
private fun MainContent(
    iconResId: Int,
    iconTint: Color?,
    title: TextReference,
    subtitle: TextReference,
    isClickableComponent: Boolean,
) {
    Row {
        Icon(
            iconResId = iconResId,
            tint = iconTint,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
        )

        SpacerW(width = TangemTheme.dimens.spacing10)

        TextsBlock(title = title, subtitle = subtitle)

        if (isClickableComponent) {
            SpacerWMax()

            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right_24),
                contentDescription = null,
                modifier = Modifier
                    .size(size = TangemTheme.dimens.size20)
                    .align(alignment = Alignment.CenterVertically),
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Composable
private fun Icon(@DrawableRes iconResId: Int, tint: Color?, modifier: Modifier = Modifier) {
    if (tint != null) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
    } else {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = modifier,
        )
    }
}

@Composable
private fun TextsBlock(title: TextReference, subtitle: TextReference) {
    Column(verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing2)) {
        Text(
            text = title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.button,
        )

        Text(
            text = subtitle.resolveReference(),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
        )
    }
}

@Composable
private fun Buttons(state: NotificationButtonsState?) {
    when (state) {
        is NotificationButtonsState.SecondaryButtonConfig -> SingleSecondaryButton(config = state)
        is NotificationButtonsState.PrimaryButtonConfig -> SinglePrimaryButton(config = state)
        is NotificationButtonsState.PairButtonsConfig -> PairButtons(config = state)
        null -> Unit
    }
}

@Composable
private fun SingleSecondaryButton(config: NotificationButtonsState.SecondaryButtonConfig) {
    SecondaryButton(
        text = config.text.resolveReference(),
        onClick = config.onClick,
        modifier = Modifier.fillMaxWidth(),
        size = TangemButtonSize.WideAction,
    )
}

@Composable
private fun SinglePrimaryButton(config: NotificationButtonsState.PrimaryButtonConfig) {
    if (config.iconResId != null) {
        PrimaryButtonIconEnd(
            text = config.text.resolveReference(),
            iconResId = config.iconResId,
            onClick = config.onClick,
            modifier = Modifier.fillMaxWidth(),
            size = TangemButtonSize.WideAction,
        )
    } else {
        PrimaryButton(
            text = config.text.resolveReference(),
            onClick = config.onClick,
            modifier = Modifier.fillMaxWidth(),
            size = TangemButtonSize.WideAction,
        )
    }
}

@Composable
private fun PairButtons(config: NotificationButtonsState.PairButtonsConfig) {
    Row(horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8)) {
        SecondaryButton(
            text = config.secondaryText.resolveReference(),
            onClick = config.onSecondaryClick,
            modifier = Modifier.weight(weight = 1f),
            size = TangemButtonSize.WideAction,
        )

        PrimaryButton(
            text = config.primaryText.resolveReference(),
            onClick = config.onPrimaryClick,
            modifier = Modifier.weight(weight = 1f),
            size = TangemButtonSize.WideAction,
        )
    }
}

@Composable
private fun CloseableIconButton(onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = onClick != null, modifier = modifier) {
        onClick ?: return@AnimatedVisibility

        /*
         * Implement a custom ripple because the design layout doesn't match the Material Design.
         * Material Icon has a size 24x24 and Material IconButton has a size 48x48,
         * but icon from Figma has a size 16x16.
         */
        Box(
            modifier = Modifier
                .size(size = TangemTheme.dimens.size40)
                .clip(shape = CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close_24),
                contentDescription = null,
                modifier = Modifier
                    .size(size = TangemTheme.dimens.size16)
                    .align(alignment = Alignment.Center),
                tint = TangemTheme.colors.icon.inactive,
            )
        }
    }
}

@Preview
@Composable
private fun Preview_Notification_Light(
    @PreviewParameter(NotificationConfigProvider::class)
    config: NotificationConfig,
) {
    TangemTheme(isDark = false) {
        Notification(config)
    }
}

@Preview
@Composable
private fun Preview_Notification_Dark(
    @PreviewParameter(NotificationConfigProvider::class) config: NotificationConfig,
) {
    TangemTheme(isDark = true) {
        Notification(config)
    }
}

private class NotificationConfigProvider : CollectionPreviewParameterProvider<NotificationConfig>(
    collection = listOf(
        NotificationConfig(
            title = TextReference.Str(value = "Development card"),
            subtitle = TextReference.Str(
                value = "The card you scanned is a development card.\nDon’t accept it as a payment.",
            ),
            iconResId = R.drawable.ic_alert_circle_24,
        ),
        NotificationConfig(
            title = TextReference.Str(value = "Some networks are unreachable"),
            subtitle = TextReference.Str(value = "Check your network connection"),
            iconResId = R.drawable.img_attention_20,
        ),
        NotificationConfig(
            title = TextReference.Str(value = "Used card"),
            subtitle = TextReference.Str(value = "The card signed transactions in the past"),
            iconResId = R.drawable.ic_alert_circle_24,
            onClick = {},
        ),
        NotificationConfig(
            title = TextReference.Str(value = "Your wallet hasn’t been backed up"),
            subtitle = TextReference.Str(value = "To protect your assets, we advise you to carry out this procedure"),
            iconResId = R.drawable.img_attention_20,
            buttonsState = NotificationButtonsState.SecondaryButtonConfig(
                text = TextReference.Str(value = "Start backup process"),
                onClick = {},
            ),
        ),
        NotificationConfig(
            title = TextReference.Str(value = "Some addresses are missing"),
            subtitle = TextReference.Str(value = "Generate addresses for 2 new networks using your card"),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationButtonsState.PrimaryButtonConfig(
                text = TextReference.Str(value = "Generate addresses"),
                iconResId = R.drawable.ic_tangem_24,
                onClick = {},
            ),
        ),
        NotificationConfig(
            title = TextReference.Str(value = "Rate the app"),
            subtitle = TextReference.Str(value = "How do you like Tangem?"),
            iconResId = R.drawable.img_attention_20,
            buttonsState = NotificationButtonsState.PairButtonsConfig(
                primaryText = TextReference.Str(value = "Love it!"),
                onPrimaryClick = {},
                secondaryText = TextReference.Str(value = "Can be better"),
                onSecondaryClick = {},
            ),
        ),
        NotificationConfig(
            title = TextReference.Str(value = "Note top up"),
            subtitle = TextReference.Str(value = "To activate card top up it with at least 1 XLM"),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationButtonsState.SecondaryButtonConfig(
                text = TextReference.Str(value = "Top up card"),
                onClick = {},
            ),
            onCloseClick = {},
        ),
    ),
)
