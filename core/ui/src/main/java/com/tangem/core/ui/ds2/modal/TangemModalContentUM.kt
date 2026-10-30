package com.tangem.core.ui.ds2.modal

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.modalinformer.TangemModalInformer
import com.tangem.core.ui.ds2.modalmenu.TangemModalMenu
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI model of a [TangemModal] content template — one subtype per template. Render any of them with
 * [TangemModalContent], or a specific one with its own overload ([TangemModalStatus], [TangemModalInformer],
 * [TangemModalMenu]). Build them with the DSL in `TangemModalDsl.kt`.
 */
@Immutable
sealed interface TangemModalContentUM {

    /**
     * Content of [TangemModalStatus]. Build it with [tangemModalStatusUM].
     *
     * @param variant Status mood. See [TangemModalStatus.Variant].
     * @param title Headline, centered.
     * @param description Secondary line under the title; `null` hides it.
     * @param titleStyle Text scale of the title block. See [TangemModalStatus.TitleStyle].
     * @param asset What the asset slot shows. See [Asset].
     * @param badge Badge in the extra-bottom slot; `null` hides it.
     * @param details Title—Value rows under the header; empty hides the details slot.
     */
    data class Status(
        val variant: TangemModalStatus.Variant,
        val title: TextReference,
        val description: TextReference? = null,
        val titleStyle: TangemModalStatus.TitleStyle = TangemModalStatus.TitleStyle.Status,
        val asset: Asset = Asset.Default,
        val badge: TangemBadgeUM? = null,
        val details: ImmutableList<Detail> = persistentListOf(),
    ) : TangemModalContentUM {

        /** Content of the asset slot. */
        @Immutable
        sealed interface Asset {

            /** The [variant]'s own [TangemModalStatus.Avatar]. */
            data object Default : Asset

            /**
             * [TangemModalStatus.Avatar] with a message-specific glyph.
             *
             * @param isTinted `false` keeps a multicolor glyph's own colors instead of the variant tint.
             */
            data class Glyph(val imageVector: ImageVector, val isTinted: Boolean = true) : Asset

            /** [Glyph] from a vector drawable resource. */
            data class GlyphRes(@DrawableRes val res: Int, val isTinted: Boolean = true) : Asset

            /** An image clipped to the 72dp avatar circle, in place of the avatar. */
            data class Image(@DrawableRes val res: Int) : Asset
        }

        /** One Title—Value row of the details slot. */
        data class Detail(val title: TextReference, val value: TextReference)
    }

    /** Content of [TangemModalInformer]: either [Plain] or [Rich]. Build it with [tangemModalInformerUM]. */
    sealed interface Informer : TangemModalContentUM {

        /** One paragraph in the primary text color. */
        data class Plain(val text: TextReference) : Informer

        /** Title—Description items, top to bottom. */
        data class Rich(val items: ImmutableList<Item>) : Informer

        /** One item of [Rich]: a title over its description. */
        data class Item(val title: TextReference, val description: TextReference)
    }

    /**
     * Content of [TangemModalMenu]. Build it with [tangemModalMenuUM].
     *
     * @param items Menu rows, top to bottom.
     */
    data class Menu(val items: ImmutableList<Item>) : TangemModalContentUM {

        /**
         * One row of the menu.
         *
         * @param title Row title.
         * @param subtitle Line under the title; `null` hides it.
         * @param avatar 40dp avatar at the start of the row; `null` hides it.
         * @param onClick Click action; call [TangemModalActionScope.dismiss] from it to close the modal.
         */
        data class Item(
            val title: TextReference,
            val subtitle: TextReference? = null,
            val avatar: TangemAvatar.UiState? = null,
            val onClick: TangemModalActionScope.() -> Unit,
        )
    }
}

/** Receiver of the click actions declared in a [TangemModalContentUM] or around it. */
@TangemModalDsl
fun interface TangemModalActionScope {

    /** Closes the modal hosting the content. */
    fun dismiss()
}