package com.tangem.core.ui.components.bottomsheets.message

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM.Button.IconOrder
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.icons.HighlightedIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun MessageBottomSheet(state: MessageBottomSheetUM, onDismissRequest: () -> Unit) {
    val stateWithOnDismiss = remember(state) {
        state.copy(
            onDismissRequest = {
                state.onDismissRequest.invoke()
                onDismissRequest()
            },
        )
    }

    val config = TangemBottomSheetConfig(
        isShown = true,
        content = stateWithOnDismiss,
        onDismissRequest = stateWithOnDismiss.onDismissRequest,
    )

    TangemModalBottomSheet(
        config = config,
        title = {
            TangemModalBottomSheetTitle(
                endIconRes = R.drawable.ic_close_24,
                onEndClick = stateWithOnDismiss.onDismissRequest,
            )
        },
        content = { content: MessageBottomSheetUM -> MessageBottomSheetContent(content) },
    )
}

@Composable
fun MessageBottomSheetContent(state: MessageBottomSheetUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        state.elements.fastForEach { element ->
            when (element) {
                is MessageBottomSheetUM.InfoBlock -> {
                    ContentContainer(
                        modifier = Modifier
                            .heightIn(min = TangemTheme.dimens.size180)
                            .fillMaxWidth()
                            .padding(horizontal = TangemTheme.dimens.spacing16)
                            .padding(bottom = 32.dp),
                        state = element,
                    )
                }
                else -> Unit
            }
        }

        ButtonsContainer(
            modifier = Modifier.fillMaxWidth(),
            closeScope = state.closeScope,
            buttons = state.elements.filterIsInstance<MessageBottomSheetUM.Button>().toPersistentList(),
        )
    }
}

@Composable
private fun ContentContainer(state: MessageBottomSheetUM.InfoBlock, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BottomSheetIconContainer(state.icon, state.iconImage)
        state.title?.let { title ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TangemTheme.dimens.spacing24)
                    .testTag(WarningBottomSheetTestTags.TITLE),
                text = title.resolveReference(),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
        }
        state.body?.let { body ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TangemTheme.dimens.spacing8)
                    .testTag(WarningBottomSheetTestTags.MESSAGE),
                text = body.resolveAnnotatedReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        state.chip?.let { chip ->
            BottomSheetChip(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing16),
                chip = chip,
            )
        }
    }
}

@Composable
private fun BottomSheetIconContainer(
    icon: MessageBottomSheetUM.Icon?,
    iconImage: MessageBottomSheetUM.IconImage?,
    modifier: Modifier = Modifier,
) {
    if (icon != null) {
        BottomSheetIcon(icon, modifier)
    } else if (iconImage != null) {
        Image(
            modifier = modifier
                .size(TangemTheme.dimens.size56)
                .clip(CircleShape),
            painter = painterResource(id = iconImage.res),
            contentDescription = null,
        )
    }
}

@Composable
private fun BottomSheetIcon(icon: MessageBottomSheetUM.Icon, modifier: Modifier = Modifier) {
    val tint = when (icon.type) {
        MessageBottomSheetUM.Icon.Type.Unspecified -> Color.Unspecified
        MessageBottomSheetUM.Icon.Type.Accent -> TangemTheme.colors.icon.accent
        MessageBottomSheetUM.Icon.Type.Informative -> TangemTheme.colors.icon.informative
        MessageBottomSheetUM.Icon.Type.Attention -> TangemTheme.colors.icon.attention
        MessageBottomSheetUM.Icon.Type.Warning -> TangemTheme.colors.icon.warning
    }

    val backgroundColor = when (icon.backgroundType) {
        MessageBottomSheetUM.Icon.BackgroundType.Unspecified -> TangemTheme.colors.icon.informative
        MessageBottomSheetUM.Icon.BackgroundType.SameAsTint -> tint
        MessageBottomSheetUM.Icon.BackgroundType.Accent -> TangemTheme.colors.icon.accent
        MessageBottomSheetUM.Icon.BackgroundType.Informative -> TangemTheme.colors.icon.informative
        MessageBottomSheetUM.Icon.BackgroundType.Attention -> TangemTheme.colors.icon.attention
        MessageBottomSheetUM.Icon.BackgroundType.Warning -> TangemTheme.colors.icon.warning
    }

    HighlightedIcon(
        modifier = modifier,
        icon = icon.res,
        iconTint = tint,
        backgroundColor = backgroundColor,
    )
}

@Composable
private fun BottomSheetChip(chip: MessageBottomSheetUM.Chip, modifier: Modifier = Modifier) {
    val color = when (chip.type) {
        MessageBottomSheetUM.Chip.Type.Unspecified -> TangemTheme.colors.text.primary1
        MessageBottomSheetUM.Chip.Type.Warning -> TangemTheme.colors.text.warning
    }

    Text(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(TangemTheme.dimens.radius16),
                color = color.copy(alpha = 0.1F),
            )
            .padding(vertical = TangemTheme.dimens.spacing4, horizontal = TangemTheme.dimens.spacing12),
        text = chip.text.resolveReference(),
        style = TangemTheme.typography.caption1,
        color = color,
    )
}

@Suppress("LongMethod")
@Composable
private fun ButtonsContainer(
    buttons: ImmutableList<MessageBottomSheetUM.Button>,
    closeScope: MessageBottomSheetUM.CloseScope,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(all = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        buttons.fastForEach { button ->
            val icon = button.icon?.let { iconResId ->
                when (button.iconOrder) {
                    IconOrder.Start -> TangemButtonIconPosition.Start(iconResId)
                    IconOrder.End -> TangemButtonIconPosition.End(iconResId)
                }
            } ?: TangemButtonIconPosition.None

            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                text = button.text?.resolveReference().orEmpty(),
                icon = icon,
                onClick = { button.onClick?.invoke(closeScope) },
                colors = if (button.isPrimary) {
                    TangemButtonsDefaults.primaryButtonColors
                } else {
                    TangemButtonsDefaults.secondaryButtonColors
                },
                enabled = true,
                showProgress = false,
                textStyle = TangemTheme.typography.subtitle1,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        MessageBottomSheet(
            messageBottomSheetUM {
                infoBlock {
                    icon(R.drawable.img_knight_shield_32) {
                        type = MessageBottomSheetUM.Icon.Type.Attention
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.SameAsTint
                    }
                    title = TextReference.Str("Title Title Title")
                    body = TextReference.Str("Body")
                    chip(text = TextReference.Str("Some chip information"))
                }
                primaryButton {
                    text = TextReference.Str("Test")
                    icon = R.drawable.ic_tangem_24
                }
                secondaryButton {
                    icon = R.drawable.ic_tangem_24
                    text = TextReference.Str("asdasd")
                    onClick {
                        closeBs()
                    }
                }
            },
            onDismissRequest = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview2() {
    TangemThemePreview {
        MessageBottomSheet(
            messageBottomSheetUM {
                infoBlock {
                    iconImage = MessageBottomSheetUM.IconImage(R.drawable.img_visa_notification)
                    title = TextReference.Str("Title Title Title")
                    body = TextReference.Str("Body")
                    chip(text = TextReference.Str("Some chip information"))
                }
                primaryButton {
                    text = TextReference.Str("Test")
                    icon = R.drawable.ic_tangem_24
                }
                secondaryButton {
                    icon = R.drawable.ic_tangem_24
                    text = TextReference.Str("asdasd")
                    onClick {
                        closeBs()
                    }
                }
            },
            onDismissRequest = {},
        )
    }
}