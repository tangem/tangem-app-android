package com.tangem.core.ui.ds2.modal

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.toPersistentList

/**
 * Marks the builders of [TangemModal] content templates, so a nested block cannot implicitly call the
 * members of the block around it (a button declared inside a status block, say).
 */
@DslMarker
annotation class TangemModalDsl

/**
 * Builds a [TangemModalContentUM.Status]. [TangemModalStatusBuilder.variant] and
 * [TangemModalStatusBuilder.title] are required.
 *
 * ```
 * tangemModalStatusUM {
 *     variant = TangemModalStatus.Variant.Warning
 *     title = resourceReference(R.string.title)
 *     description = resourceReference(R.string.description)
 *     glyph(Icons.ic_shield_28)
 *     badge(text = resourceReference(R.string.badge), status = TangemBadge.Status.Warning)
 *     detail(title = resourceReference(R.string.fee), value = stringReference("$5.00"))
 * }
 * ```
 */
fun tangemModalStatusUM(init: TangemModalStatusBuilder.() -> Unit): TangemModalContentUM.Status =
    TangemModalStatusBuilder().apply(init).build()

/**
 * Builds a [TangemModalContentUM.Informer]: set [TangemModalInformerBuilder.text] for the Plain variant, or
 * add [items][TangemModalInformerBuilder.item] for the Rich one — not both.
 */
fun tangemModalInformerUM(init: TangemModalInformerBuilder.() -> Unit): TangemModalContentUM.Informer =
    TangemModalInformerBuilder().apply(init).build()

/**
 * Builds a [TangemModalContentUM.Menu].
 *
 * ```
 * tangemModalMenuUM {
 *     item {
 *         title = resourceReference(R.string.title)
 *         subtitle = resourceReference(R.string.subtitle)
 *         avatar = TangemAvatar.UiState.Icon(Icons.ic_wallet_24)
 *         onClick { openWallet(); dismiss() }
 *     }
 * }
 * ```
 */
fun tangemModalMenuUM(init: TangemModalMenuBuilder.() -> Unit): TangemModalContentUM.Menu =
    TangemModalMenuBuilder().apply(init).build()

@TangemModalDsl
class TangemModalStatusBuilder internal constructor() {

    lateinit var variant: TangemModalStatus.Variant
    lateinit var title: TextReference
    var description: TextReference? = null
    var titleStyle: TangemModalStatus.TitleStyle = TangemModalStatus.TitleStyle.Status

    private var asset: TangemModalContentUM.Status.Asset = TangemModalContentUM.Status.Asset.Default
    private var badge: TangemBadgeUM? = null
    private val details = mutableListOf<TangemModalContentUM.Status.Detail>()

    fun glyph(imageVector: ImageVector, isTinted: Boolean = true) {
        asset = TangemModalContentUM.Status.Asset.Glyph(imageVector = imageVector, isTinted = isTinted)
    }

    fun glyph(@DrawableRes res: Int, isTinted: Boolean = true) {
        asset = TangemModalContentUM.Status.Asset.GlyphRes(res = res, isTinted = isTinted)
    }

    fun image(@DrawableRes res: Int) {
        asset = TangemModalContentUM.Status.Asset.Image(res = res)
    }

    fun badge(text: TextReference, status: TangemBadge.Status = TangemBadge.Status.Neutral) {
        badge = TangemBadgeUM(text = text, status = status)
    }

    fun badge(badge: TangemBadgeUM) {
        this.badge = badge
    }

    fun detail(title: TextReference, value: TextReference) {
        details += TangemModalContentUM.Status.Detail(title = title, value = value)
    }

    internal fun build(): TangemModalContentUM.Status = TangemModalContentUM.Status(
        variant = variant,
        title = title,
        description = description,
        titleStyle = titleStyle,
        asset = asset,
        badge = badge,
        details = details.toPersistentList(),
    )
}

@TangemModalDsl
class TangemModalInformerBuilder internal constructor() {

    /** Paragraph of the Plain variant. */
    var text: TextReference? = null

    private val items = mutableListOf<TangemModalContentUM.Informer.Item>()

    /** Adds a Title—Description item of the Rich variant. */
    fun item(title: TextReference, description: TextReference) {
        items += TangemModalContentUM.Informer.Item(title = title, description = description)
    }

    internal fun build(): TangemModalContentUM.Informer {
        val text = text
        return when {
            text != null && items.isEmpty() -> TangemModalContentUM.Informer.Plain(text = text)
            text == null && items.isNotEmpty() -> TangemModalContentUM.Informer.Rich(items = items.toPersistentList())
            else -> error("An informer takes either text (Plain) or items (Rich)")
        }
    }
}

@TangemModalDsl
class TangemModalMenuBuilder internal constructor() {

    private val items = mutableListOf<TangemModalContentUM.Menu.Item>()

    fun item(init: TangemModalMenuItemBuilder.() -> Unit) {
        items += TangemModalMenuItemBuilder().apply(init).build()
    }

    internal fun build(): TangemModalContentUM.Menu = TangemModalContentUM.Menu(items = items.toPersistentList())
}

@TangemModalDsl
class TangemModalMenuItemBuilder internal constructor() {

    lateinit var title: TextReference
    var subtitle: TextReference? = null
    var avatar: TangemAvatar.UiState? = null

    /** Click action; without one the row just closes the modal. */
    var onClick: (TangemModalActionScope.() -> Unit)? = null

    fun onClick(block: TangemModalActionScope.() -> Unit) {
        onClick = block
    }

    internal fun build(): TangemModalContentUM.Menu.Item = TangemModalContentUM.Menu.Item(
        title = title,
        subtitle = subtitle,
        avatar = avatar,
        onClick = onClick ?: { dismiss() },
    )
}