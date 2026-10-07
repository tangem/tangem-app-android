package com.tangem.features.collectibles.impl.confirmorder.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CollectiblesConfirmOrderUM {

    val onBackClick: () -> Unit

    data class Loading(override val onBackClick: () -> Unit) : CollectiblesConfirmOrderUM

    /** @property isReloading a retry is in flight; the error stays with a spinner in its button */
    data class Error(
        override val onBackClick: () -> Unit,
        val message: TextReference,
        val onReloadClick: () -> Unit,
        val isReloading: Boolean = false,
    ) : CollectiblesConfirmOrderUM

    data class Content(
        override val onBackClick: () -> Unit,
        val order: OrderUM,
        val onApproveClick: () -> Unit,
    ) : CollectiblesConfirmOrderUM

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