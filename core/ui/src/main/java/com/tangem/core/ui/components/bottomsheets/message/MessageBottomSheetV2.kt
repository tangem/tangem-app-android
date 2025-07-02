package com.tangem.core.ui.components.bottomsheets.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2.Button.IconOrder
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun MessageBottomSheetV2(state: MessageBottomSheetUMV2, onDismissRequest: () -> Unit) {
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
        content = { content: MessageBottomSheetUMV2 -> MessageBottomSheetV2Content(content) },
    )
}

@Composable
fun MessageBottomSheetV2Content(state: MessageBottomSheetUMV2, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        state.elements.fastForEach {
            when (it) {
                is MessageBottomSheetUMV2.InfoBlock -> {
                    ContentContainer(
                        modifier = Modifier
                            .heightIn(min = TangemTheme.dimens.size180)
                            .fillMaxWidth()
                            .padding(horizontal = TangemTheme.dimens.spacing16)
                            .padding(bottom = 32.dp),
                        state = it,
                    )
                }
                else -> Unit
            }
        }

        ButtonsContainer(
            modifier = Modifier.fillMaxWidth(),
            closeScope = state.closeScope,
            buttons = state.elements.filterIsInstance<MessageBottomSheetUMV2.Button>().toPersistentList(),
        )
    }
}

@Composable
private fun ContentContainer(state: MessageBottomSheetUMV2.InfoBlock, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        state.icon?.let {
            BottomSheetIcon(it)
        }
        state.title?.let {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TangemTheme.dimens.spacing24),
                text = it.resolveReference(),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
        }
        state.body?.let {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TangemTheme.dimens.spacing8),
                text = it.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        state.chip?.let {
            BottomSheetChip(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing16),
                chip = it,
            )
        }
    }
}

@Composable
private fun BottomSheetIcon(icon: MessageBottomSheetUMV2.Icon, modifier: Modifier = Modifier) {
    val tint = when (icon.type) {
        MessageBottomSheetUMV2.Icon.Type.Unspecified -> Color.Unspecified
        MessageBottomSheetUMV2.Icon.Type.Accent -> TangemTheme.colors.icon.accent
        MessageBottomSheetUMV2.Icon.Type.Informative -> TangemTheme.colors.icon.informative
        MessageBottomSheetUMV2.Icon.Type.Attention -> TangemTheme.colors.icon.attention
        MessageBottomSheetUMV2.Icon.Type.Warning -> TangemTheme.colors.icon.warning
    }

    val backgroundColor = when (icon.backgroundType) {
        MessageBottomSheetUMV2.Icon.BackgroundType.Unspecified -> TangemTheme.colors.icon.informative
        MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint -> tint
    }

    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size56)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.1F)),
        contentAlignment = Alignment.Center,
        content = {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size32),
                painter = painterResource(icon.res),
                contentDescription = null,
                tint = tint,
            )
        },
    )
}

@Composable
private fun BottomSheetChip(chip: MessageBottomSheetUMV2.Chip, modifier: Modifier = Modifier) {
    val color = when (chip.type) {
        MessageBottomSheetUMV2.Chip.Type.Unspecified -> TangemTheme.colors.text.primary1
        MessageBottomSheetUMV2.Chip.Type.Warning -> TangemTheme.colors.text.warning
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
    buttons: ImmutableList<MessageBottomSheetUMV2.Button>,
    closeScope: MessageBottomSheetUMV2.CloseScope,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(all = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        buttons.fastForEach { button ->
            val icon = button.icon?.let {
                when (button.iconOrder) {
                    IconOrder.Start -> TangemButtonIconPosition.Start(it)
                    IconOrder.End -> TangemButtonIconPosition.End(it)
                }
            } ?: TangemButtonIconPosition.None

            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                text = button.text?.resolveReference() ?: "",
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

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        MessageBottomSheetV2(
            messageBottomSheetUM {
                infoBlock {
                    icon(R.drawable.img_knight_shield_32) {
                        type = MessageBottomSheetUMV2.Icon.Type.Attention
                        backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
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