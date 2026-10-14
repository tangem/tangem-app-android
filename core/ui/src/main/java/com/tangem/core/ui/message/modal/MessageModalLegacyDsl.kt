package com.tangem.core.ui.message.modal

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modal.TangemModalDsl
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orEmpty

/*
 * The messageBottomSheetUM grammar inside messageModalUM, so a MessageBottomSheet call site migrates by switching
 * the entry point. Delete this file once no call site uses it.
 */

@Deprecated(message = "Use dismiss()", replaceWith = ReplaceWith("dismiss()"))
fun TangemModalActionScope.closeBs() {
    dismiss()
}

/** `infoBlock { }` of the `messageBottomSheetUM` grammar; builds a [TangemModalContentUM.Status]. */
@TangemModalDsl
class MessageModalInfoBlockBuilder internal constructor() {

    var title: TextReference? = null
    var body: TextReference? = null

    private var icon: MessageBottomSheetUM.Icon? = null
    private var vector: MessageBottomSheetUM.Vector? = null

    @DrawableRes
    private var iconImage: Int? = null
    private var chip: MessageBottomSheetUM.Chip? = null

    fun icon(@DrawableRes res: Int, init: MessageBottomSheetUM.Icon.() -> Unit = {}) {
        icon = MessageBottomSheetUM.Icon(res).apply(init)
    }

    fun vector(imageVector: ImageVector, init: MessageBottomSheetUM.Vector.() -> Unit = {}) {
        vector = MessageBottomSheetUM.Vector(imageVector).apply(init)
    }

    fun iconImage(@DrawableRes res: Int) {
        iconImage = res
    }

    fun chip(text: TextReference, init: MessageBottomSheetUM.Chip.() -> Unit = {}) {
        chip = MessageBottomSheetUM.Chip(text).apply(init)
    }

    /** An untyped icon is a multicolor drawable, so it keeps its own colors instead of the variant tint. */
    internal fun build(): TangemModalContentUM.Status {
        val icon = icon
        val vector = vector
        val iconImage = iconImage
        val (variant, asset) = when {
            icon != null -> icon.type.variant() to TangemModalContentUM.Status.Asset.GlyphRes(
                res = icon.res,
                isTinted = icon.type != MessageBottomSheetUM.Icon.Type.Unspecified,
            )
            vector != null -> vector.type.variant() to TangemModalContentUM.Status.Asset.Glyph(
                imageVector = vector.imageVector,
                isTinted = vector.type != MessageBottomSheetUM.Vector.Type.Unspecified,
            )
            iconImage != null -> TangemModalStatus.Variant.Info to TangemModalContentUM.Status.Asset.Image(iconImage)
            else -> TangemModalStatus.Variant.Info to TangemModalContentUM.Status.Asset.Default
        }
        return TangemModalContentUM.Status(
            variant = variant,
            title = title.orEmpty(),
            description = body,
            asset = asset,
            badge = chip?.let { legacyChip ->
                TangemBadgeUM(text = legacyChip.text, status = legacyChip.type.badgeStatus())
            },
        )
    }
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

private fun MessageBottomSheetUM.Chip.Type.badgeStatus(): TangemBadge.Status = when (this) {
    MessageBottomSheetUM.Chip.Type.Unspecified -> TangemBadge.Status.Neutral
    MessageBottomSheetUM.Chip.Type.Warning -> TangemBadge.Status.Warning
}