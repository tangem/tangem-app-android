package com.tangem.features.collectibles.impl.packopening.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CollectiblesPackOpeningUM {

    val onBackClick: () -> Unit
    val onCloseClick: () -> Unit

    data class Loading(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
    ) : CollectiblesPackOpeningUM

    /** @property isReloading a retry is in flight; the error stays with a spinner in its button */
    data class Error(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
        val message: TextReference,
        val onReloadClick: () -> Unit,
        val isReloading: Boolean = false,
    ) : CollectiblesPackOpeningUM

    /**
     * @property isPackOpened the card is known, so the pack is thrown
     * @property isNavigationVisible false while a purchase runs; a failure brings it back
     */
    data class Content(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
        val order: OrderUM,
        val onApproveClick: () -> Unit,
        val isPackOpened: Boolean,
        val isNavigationVisible: Boolean,
    ) : CollectiblesPackOpeningUM

    /** @property packImageUrl null renders the placeholder */
    data class OrderUM(
        val rows: ImmutableList<PriceRowUM>,
        val button: ButtonUM,
        val packImageUrl: String? = null,
    )

    data class ButtonUM(val textRef: TextReference, val isEnabled: Boolean)

    /** @property isEmphasised the total row, not dimmed */
    data class PriceRowUM(
        val labelRef: TextReference,
        val value: String,
        val isEmphasised: Boolean = false,
        val hasDivider: Boolean = true,
    )
}