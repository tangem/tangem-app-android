package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

/**
 * State of the "Get token" bottom sheet shown after tapping the balance-block "Add funds" button.
 *
 * Stays [Loading] while [com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase] hasn't yet
 * emitted the action list; the sheet renders a spinner in the tail of each row. Once actions
 * arrive the state becomes [Content]; unavailable actions stay visible but with
 * [Row.isEnabled] = false. Rows whose action is absent from the response are dropped (null).
 */
@Immutable
internal sealed interface AddFundsUM : TangemBottomSheetConfigContent {

    @Immutable
    data object Loading : AddFundsUM

    @Immutable
    data class Content(
        val buy: Row?,
        val swap: Row?,
        val receive: Row?,
    ) : AddFundsUM

    @Immutable
    data class Row(
        val isLoading: Boolean,
        val isEnabled: Boolean,
        val onClick: () -> Unit,
        val onLongClick: (() -> Unit)? = null,
    )
}