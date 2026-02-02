package com.tangem.core.ui.ds.message

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.flicker
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.notifications.NotificationConfig.ButtonsState
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.persistentListOf

/**
 * Tangem message component that displays a message based on the provided [TangemMessageUM].
 * [Message](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8455-81318&m=dev)
 *
 * @param messageUM   Data model containing message properties.
 * @param modifier    Modifier to be applied to the message component.
 * @param content     Optional composable content to be displayed alongside the title and subtitle.
 */
@Composable
fun TangemMessage(
    messageUM: TangemMessageUM,
    modifier: Modifier = Modifier,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    TangemMessage(
        modifier = modifier,
        title = messageUM.title,
        subtitle = messageUM.subtitle,
        messageEffect = messageUM.messageEffect,
        isCentered = messageUM.isCentered,
        content = content,
        onCloseClick = messageUM.onCloseClick,
        buttons = {
            messageUM.buttonsUM.fastForEach { buttonUM ->
                TangemButton(
                    buttonUM = buttonUM.tangemButtonUM,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

/**
 * Tangem message component that displays a notification based on the provided [NotificationConfig].
 * [Message](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8455-81318&m=dev)
 *
 * @param config   Configuration for the notification message.
 * @param modifier Modifier to be applied to the message component.
 *
 * @see NotificationConfig for more details.
 * @see com.tangem.core.ui.components.notifications.Notification for legacy component.
 */
@Composable
fun TangemMessage(config: NotificationConfig, modifier: Modifier = Modifier) {
    val buttonState = config.buttonsState
    TangemMessage(
        title = config.title,
        subtitle = config.subtitle,
        modifier = modifier,
        content = {
            val iconTint = when (config.iconTint) {
                NotificationConfig.IconTint.Unspecified -> null
                NotificationConfig.IconTint.Accent -> TangemTheme.colors2.graphic.status.accent
                NotificationConfig.IconTint.Attention -> TangemTheme.colors2.graphic.status.attention
                NotificationConfig.IconTint.Warning -> TangemTheme.colors2.graphic.status.warning
            }
            if (iconTint == null) {
                Image(
                    painter = painterResource(config.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(config.iconSize),
                )
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(config.iconResId),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(config.iconSize),
                )
            }
        },
        buttons = if (buttonState != null) {
            {
                TangemMessageLegacyButtons(buttonState = buttonState)
            }
        } else {
            null
        },
    )
}

/**
 * Tangem message component that displays a message with optional title, subtitle, content, and buttons.
 * [Message](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8455-81318&m=dev)
 *
 * @param modifier      Modifier to be applied to the message component.
 * @param title         Optional title of the message.
 * @param subtitle      Optional subtitle of the message.
 * @param messageEffect Effect to be applied to the message background.
 * @param content       Optional composable content to be displayed alongside the title and subtitle.
 * @param buttons       Optional composable buttons to be displayed below the message.
 * @param isCentered    Flag indicating whether the content should be centered horizontally.
 */
@Composable
fun TangemMessage(
    modifier: Modifier = Modifier,
    title: TextReference? = null,
    subtitle: TextReference? = null,
    messageEffect: TangemMessageEffect = TangemMessageEffect.None,
    content: (@Composable RowScope.() -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
    isCentered: Boolean = false,
) {
    val alignment = if (isCentered) {
        Alignment.CenterHorizontally
    } else {
        Alignment.Start
    }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .messageEffectBackground(
                    messageEffect = messageEffect,
                    radius = TangemTheme.dimens2.x6,
                ),
        )
        Column(
            horizontalAlignment = alignment,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
            modifier = Modifier
                .padding(TangemTheme.dimens2.x3)
                .fillMaxWidth(),
        ) {
            TangemMessageContent(
                title = title,
                subtitle = subtitle,
                alignment = alignment,
                content = content,
            )
            if (buttons != null && !isCentered) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    buttons()
                }
            }
        }
        if (onCloseClick != null) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_close_new_20),
                contentDescription = null,
                tint = TangemTheme.colors2.text.neutral.secondary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(TangemTheme.dimens2.x3)
                    .size(TangemTheme.dimens2.x5)
                    .clickableSingle(onClick = onCloseClick),
            )
        }
    }
}

@Composable
private fun TangemMessageContent(
    title: TextReference? = null,
    subtitle: TextReference? = null,
    alignment: Alignment.Horizontal = Alignment.Start,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        modifier = Modifier.padding(TangemTheme.dimens2.x1),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = alignment,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
        ) {
            if (title != null) {
                Text(
                    text = title.resolveAnnotatedReference(),
                    style = TangemTheme.typography2.bodySemibold16,
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle.resolveAnnotatedReference(),
                    style = TangemTheme.typography2.captionSemibold12,
                    color = TangemTheme.colors2.text.neutral.secondary,
                )
            }
        }
        content?.invoke(this)
    }
}

@Composable
private fun RowScope.TangemMessageLegacyButtons(buttonState: ButtonsState) {
    when (buttonState) {
        is ButtonsState.PrimaryButtonConfig -> PrimaryTangemButton(
            text = buttonState.text,
            descriptionText = buttonState.additionalText,
            iconRes = buttonState.iconResId,
            onClick = buttonState.onClick,
            iconPosition = TangemButtonIconPosition.End,
            shape = TangemButtonShape.Rounded,
            size = TangemButtonSize.X9,
            state = if (buttonState.shouldShowProgress) {
                TangemButtonState.Loading
            } else {
                TangemButtonState.Default
            },
            modifier = Modifier.weight(1f),
        )
        is ButtonsState.SecondaryButtonConfig -> PrimaryInverseTangemButton(
            text = buttonState.text,
            iconRes = buttonState.iconResId,
            onClick = buttonState.onClick,
            iconPosition = TangemButtonIconPosition.End,
            shape = TangemButtonShape.Rounded,
            size = TangemButtonSize.X9,
            state = if (buttonState.shouldShowProgress) {
                TangemButtonState.Loading
            } else {
                TangemButtonState.Default
            },
            modifier = Modifier.weight(1f),
        )
        is ButtonsState.PairButtonsConfig -> {
            PrimaryInverseTangemButton(
                text = buttonState.primaryText,
                onClick = buttonState.onPrimaryClick,
                iconPosition = TangemButtonIconPosition.End,
                shape = TangemButtonShape.Rounded,
                size = TangemButtonSize.X9,
                modifier = Modifier.weight(1f),
            )
            PrimaryTangemButton(
                text = buttonState.secondaryText,
                onClick = buttonState.onSecondaryClick,
                iconPosition = TangemButtonIconPosition.End,
                shape = TangemButtonShape.Rounded,
                size = TangemButtonSize.X9,
                modifier = Modifier.weight(1f),
            )
        }
        is ButtonsState.SecondaryPairButtonsConfig -> {
            PrimaryInverseTangemButton(
                text = buttonState.leftText,
                onClick = buttonState.onLeftClick,
                iconPosition = TangemButtonIconPosition.End,
                shape = TangemButtonShape.Rounded,
                size = TangemButtonSize.X9,
                modifier = Modifier.weight(1f),
            )
            PrimaryInverseTangemButton(
                text = buttonState.rightText,
                onClick = buttonState.onRightClick,
                iconPosition = TangemButtonIconPosition.End,
                shape = TangemButtonShape.Rounded,
                size = TangemButtonSize.X9,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemMessage_Preview(@PreviewParameter(TangemMessagePreviewProvider::class) params: TangemMessageUM) {
    TangemThemePreviewRedesign {
        TangemMessage(
            messageUM = params,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private class TangemMessagePreviewProvider : PreviewParameterProvider<TangemMessageUM> {
    override val values: Sequence<TangemMessageUM>
        get() = sequenceOf(
            TangemMessageUM(
                title = stringReference("Title text"),
                subtitle = stringReference("Subtext"),
                messageEffect = TangemMessageEffect.None,
                isCentered = true,
            ),
            TangemMessageUM(
                title = stringReference("Title text"),
                subtitle = stringReference("Subtext"),
                messageEffect = TangemMessageEffect.Magic,
                isCentered = false,
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = stringReference("Button"),
                        type = TangemButtonType.PrimaryInverse,
                        onClick = {},
                    ),
                    TangemMessageButtonUM(
                        text = stringReference("Button"),
                        type = TangemButtonType.Primary,
                        iconRes = R.drawable.ic_tangem_24,
                        onClick = {},
                    ),
                ),
            ),
            TangemMessageUM(
                title = stringReference("Title text"),
                subtitle = stringReference("Subtext"),
                messageEffect = TangemMessageEffect.Card,
                isCentered = false,
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = stringReference("Button"),
                        type = TangemButtonType.Primary,
                        onClick = {},
                    ),
                ),
            ),
            TangemMessageUM(
                title = stringReference("Title text"),
                subtitle = stringReference("Subtext"),
                messageEffect = TangemMessageEffect.Warning,
                isCentered = true,
                onCloseClick = {},
            ),
        )
}
// endregion

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemMessage2_Preview() {
    TangemThemePreviewRedesign {
        TangemMessage(
            title = stringReference("Title text"),
            subtitle = stringReference("Subtext"),
            messageEffect = TangemMessageEffect.Magic,
            isCentered = false,
            content = {
                Box(
                    modifier = Modifier
                        .size(TangemTheme.dimens2.x10)
                        .clip(RoundedCornerShape(TangemTheme.dimens2.x2))
                        .flicker(isFlickering = true)
                        .background(TangemTheme.colors2.text.neutral.primary),
                )
            },
            buttons = {
                PrimaryInverseTangemButton(
                    text = stringReference("Button"),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    size = TangemButtonSize.X9,
                    shape = TangemButtonShape.Rounded,
                )
                PrimaryTangemButton(
                    text = stringReference("Button"),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    size = TangemButtonSize.X9,
                    shape = TangemButtonShape.Rounded,
                )
            },
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(TangemTheme.dimens2.x1)
                .fillMaxWidth(),
        )
    }
}
// endregion

// region Preview Legacy
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemMessageLegacy_Preview(
    @PreviewParameter(TangemMessageLegacyPreviewProvider::class) params: NotificationConfig,
) {
    TangemThemePreviewRedesign {
        TangemMessage(
            config = params,
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(TangemTheme.dimens2.x1)
                .fillMaxWidth(),
        )
    }
}

private class TangemMessageLegacyPreviewProvider : PreviewParameterProvider<NotificationConfig> {
    override val values: Sequence<NotificationConfig>
        get() = sequenceOf(
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
                subtitle = TextReference.Str(
                    value = "To protect your assets, we advise you to carry out this procedure",
                ),
                iconResId = R.drawable.img_attention_20,
                buttonsState = ButtonsState.SecondaryButtonConfig(
                    text = TextReference.Str(value = "Start backup process"),
                    onClick = {},
                ),
            ),
            NotificationConfig(
                title = TextReference.Str(value = "Some addresses are missing"),
                subtitle = TextReference.Str(value = "Generate addresses for 2 new networks using your card"),
                iconResId = R.drawable.ic_alert_circle_24,
                buttonsState = ButtonsState.PrimaryButtonConfig(
                    text = TextReference.Str(value = "Generate addresses"),
                    iconResId = R.drawable.ic_tangem_24,
                    onClick = {},
                ),
            ),
            NotificationConfig(
                title = TextReference.Str(value = "Rate the app"),
                subtitle = TextReference.Str(value = "How do you like Tangem?"),
                iconResId = R.drawable.img_attention_20,
                buttonsState = ButtonsState.PairButtonsConfig(
                    primaryText = TextReference.Str(value = "Love it!"),
                    onPrimaryClick = {},
                    secondaryText = TextReference.Str(value = "Can be better"),
                    onSecondaryClick = {},
                ),
            ),
            NotificationConfig(
                title = TextReference.Str(value = "Rate the app"),
                subtitle = TextReference.Str(value = "How do you like Tangem?"),
                iconResId = R.drawable.img_attention_20,
                buttonsState = ButtonsState.SecondaryPairButtonsConfig(
                    leftText = TextReference.Str(value = "Love it!"),
                    onLeftClick = {},
                    rightText = TextReference.Str(value = "Can be better"),
                    onRightClick = {},
                ),
            ),
            NotificationConfig(
                title = TextReference.Str(value = "Note top up"),
                subtitle = TextReference.Str(value = "To activate card top up it with at least 1 XLM"),
                iconResId = R.drawable.ic_alert_circle_24,
                buttonsState = ButtonsState.SecondaryButtonConfig(
                    text = TextReference.Str(value = "Top up card"),
                    onClick = {},
                ),
                onCloseClick = {},
            ),
            NotificationConfig(
                subtitle = resourceReference(id = R.string.information_generated_with_ai),
                iconResId = R.drawable.ic_magic_28,
            ),
            NotificationConfig(
                title = resourceReference(R.string.notification_sepa_title),
                subtitle = resourceReference(R.string.notification_sepa_text),
                iconResId = R.drawable.img_notification_sepa,
                buttonsState = ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(R.string.notification_sepa_button),
                    onClick = { },
                ),
                iconSize = 54.dp,
            ),
        )
}
// endregion