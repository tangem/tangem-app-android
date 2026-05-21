package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

/**
 * State of the "Transfer" bottom sheet shown after tapping the balance-block "Transfer" button.
 *
 * Stays [Loading] while [com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase] hasn't yet
 * emitted the action list; the sheet renders a spinner in the tail of each row. Once actions
 * arrive the state becomes [Content]; unavailable actions stay visible but with
 * [Row.isEnabled] = false. Rows whose action is absent from the response are dropped (null).
 */
@Immutable
internal sealed interface TransferUM : TangemBottomSheetConfigContent {

    @Immutable
    data object Loading : TransferUM

    @Immutable
    data class Content(
        val send: Row?,
        val swap: Row?,
        val sell: Row?,
    ) : TransferUM

    @Immutable
    data class Row(
        val isLoading: Boolean,
        val isEnabled: Boolean,
        val onClick: () -> Unit,
    )
}