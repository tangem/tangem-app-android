package com.tangem.core.ui.components.bottomsheets.message

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orEmpty
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun MessageBottomSheetV3(state: MessageBottomSheetUM, onDismissRequest: () -> Unit) {
    val stateWithOnDismiss = remember(state) {
        state.copy(
            onDismissRequest = {
                state.onDismissRequest.invoke()
                onDismissRequest()
            },
        )
    }

    TangemModal<MessageBottomSheetUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            content = stateWithOnDismiss,
            onDismissRequest = stateWithOnDismiss.onDismissRequest,
        ),
        onBack = stateWithOnDismiss.onDismissRequest,
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(
                endButton = {
                    TangemButton.Close(
                        modifier = Modifier.testTag(WarningBottomSheetTestTags.CLOSE_BUTTON),
                        onClick = stateWithOnDismiss.onDismissRequest,
                    )
                },
            )
        },
    ) { content ->
        MessageBottomSheetContentV3(state = content, modifier = Modifier.weight(weight = 1f, fill = false))
    }
}

@Composable
fun MessageBottomSheetContentV3(state: MessageBottomSheetUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            state.elements.fastForEach { element ->
                if (element is MessageBottomSheetUM.InfoBlock) InfoBlockStatus(block = element)
            }
        }
        Buttons(
            buttons = state.elements.filterIsInstance<MessageBottomSheetUM.Button>().toPersistentList(),
            closeScope = state.closeScope,
        )
    }
}

@Composable
private fun InfoBlockStatus(block: MessageBottomSheetUM.InfoBlock) {
    val variant = block.variant()
    TangemModalStatus(
        variant = variant,
        title = block.title.orEmpty(),
        description = block.body,
        asset = { InfoBlockAsset(block = block, variant = variant) },
        extraBottom = block.chip?.let { chip ->
            { TangemBadge(text = chip.text, status = chip.badgeStatus()) }
        },
    )
}

@Composable
private fun InfoBlockAsset(block: MessageBottomSheetUM.InfoBlock, variant: TangemModalStatus.Variant) {
    val icon = block.icon
    val vector = block.vector
    val iconImage = block.iconImage
    val modifier = Modifier.testTag(WarningBottomSheetTestTags.ICON)
    when {
        icon != null -> GlyphAvatar(
            variant = variant,
            imageVector = ImageVector.vectorResource(id = icon.res),
            hasOwnColors = icon.type == MessageBottomSheetUM.Icon.Type.Unspecified,
            modifier = modifier,
        )
        vector != null -> GlyphAvatar(
            variant = variant,
            imageVector = vector.imageVector,
            hasOwnColors = vector.type == MessageBottomSheetUM.Vector.Type.Unspecified,
            modifier = modifier,
        )
        iconImage != null -> Image(
            modifier = modifier
                .size(72.dp)
                .clip(CircleShape),
            painter = painterResource(id = iconImage.res),
            contentDescription = null,
        )
        // No icon in the message: an empty slot, so the variant's default (a spinner) does not stand in.
        else -> Box {}
    }
}

/** An untyped icon is a multicolor drawable (as in V2), so it keeps its own colors instead of the variant tint. */
@Composable
private fun GlyphAvatar(
    variant: TangemModalStatus.Variant,
    imageVector: ImageVector,
    hasOwnColors: Boolean,
    modifier: Modifier = Modifier,
) {
    if (hasOwnColors) {
        TangemModalStatus.Avatar(
            variant = variant,
            imageVector = imageVector,
            modifier = modifier,
            tint = Color.Unspecified,
        )
    } else {
        TangemModalStatus.Avatar(variant = variant, imageVector = imageVector, modifier = modifier)
    }
}

private fun MessageBottomSheetUM.InfoBlock.variant(): TangemModalStatus.Variant {
    icon?.let { return it.type.variant() }
    vector?.let { return it.type.variant() }
    return TangemModalStatus.Variant.Neutral
}

private fun MessageBottomSheetUM.Icon.Type.variant(): TangemModalStatus.Variant = when (this) {
    MessageBottomSheetUM.Icon.Type.Unspecified -> TangemModalStatus.Variant.Neutral
    MessageBottomSheetUM.Icon.Type.Accent,
    MessageBottomSheetUM.Icon.Type.Informative,
    -> TangemModalStatus.Variant.Info
    MessageBottomSheetUM.Icon.Type.Attention -> TangemModalStatus.Variant.Warning
    MessageBottomSheetUM.Icon.Type.Warning -> TangemModalStatus.Variant.Error
}

private fun MessageBottomSheetUM.Vector.Type.variant(): TangemModalStatus.Variant = when (this) {
    MessageBottomSheetUM.Vector.Type.Unspecified -> TangemModalStatus.Variant.Neutral
    MessageBottomSheetUM.Vector.Type.Accent,
    MessageBottomSheetUM.Vector.Type.Informative,
    -> TangemModalStatus.Variant.Info
    MessageBottomSheetUM.Vector.Type.Attention -> TangemModalStatus.Variant.Warning
    MessageBottomSheetUM.Vector.Type.Warning -> TangemModalStatus.Variant.Error
    MessageBottomSheetUM.Vector.Type.Success -> TangemModalStatus.Variant.Success
}

private fun MessageBottomSheetUM.Chip.badgeStatus(): TangemBadge.Status = when (type) {
    MessageBottomSheetUM.Chip.Type.Unspecified -> TangemBadge.Status.Neutral
    MessageBottomSheetUM.Chip.Type.Warning -> TangemBadge.Status.Warning
}

@Composable
private fun Buttons(buttons: ImmutableList<MessageBottomSheetUM.Button>, closeScope: MessageBottomSheetUM.CloseScope) {
    if (buttons.isEmpty()) return
    TangemFooter(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = TangemFooter.ModalWindowInsets,
        fadeVariant = TangemFade.Variant.Hard,
        fadeEnabled = false,
    ) {
        TangemButtonGroup(modifier = Modifier.fillMaxWidth()) {
            buttons.fastForEach { button ->
                val icon = button.icon?.let(TangemIconUM::Icon)
                val isIconAtStart = button.iconOrder == MessageBottomSheetUM.Button.IconOrder.Start
                TangemButton(
                    modifier = Modifier.testTag(
                        if (button.isPrimary) {
                            WarningBottomSheetTestTags.BUTTON_PRIMARY
                        } else {
                            WarningBottomSheetTestTags.BUTTON_SECONDARY
                        },
                    ),
                    onClick = { button.onClick?.invoke(closeScope) },
                    text = button.text ?: TextReference.EMPTY,
                    iconStart = if (isIconAtStart) icon else null,
                    iconEnd = if (isIconAtStart) null else icon,
                    variant = if (button.isPrimary) TangemButton.Variant.Primary else TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                )
            }
        }
    }
}

// region Preview
// The previews show the modal's body on a static card: TangemModal's preview path still opens a Material3
// sheet window, which layoutlib cannot compose.
@Preview(showBackground = true, widthDp = 360, heightDp = 520)
@Preview(showBackground = true, widthDp = 360, heightDp = 520, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessageBottomSheetV3Preview() {
    TangemThemePreviewRedesign {
        PreviewCard(
            state = messageBottomSheetUM {
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
                    text = TextReference.Str("Close")
                    onClick { closeBs() }
                }
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 520)
@Preview(showBackground = true, widthDp = 360, heightDp = 520, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessageBottomSheetV3ImagePreview() {
    TangemThemePreviewRedesign {
        PreviewCard(
            state = messageBottomSheetUM {
                infoBlock {
                    iconImage = MessageBottomSheetUM.IconImage(R.drawable.img_visa_notification)
                    title = TextReference.Str("Title Title Title")
                    body = TextReference.Str("Body")
                }
                primaryButton { text = TextReference.Str("Got it") }
            },
        )
    }
}

@Composable
private fun PreviewCard(state: MessageBottomSheetUM) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = {}) })
        MessageBottomSheetContentV3(state = state, modifier = Modifier.weight(weight = 1f, fill = false))
    }
}
// endregion