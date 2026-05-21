package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable

/**
 * State of the Buy / Swap / Receive rows rendered in place of the balance-block action buttons
 * when the token balance is zero.
 *
 * Stays [Loading] while [com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase] hasn't yet
 * emitted the action list. Once actions arrive the state becomes [Content], and each row
 * carries [Row.isEnabled] reflecting its current `ScenarioUnavailabilityReason`. Disabled rows
 * stay visible but ignore clicks.
 */
@Immutable
internal sealed interface ZeroBalanceActionsUM {

    @Immutable
    data object Loading : ZeroBalanceActionsUM

    @Immutable
    data class Content(
        val buy: Row?,
        val swap: Row?,
        val receive: Row?,
    ) : ZeroBalanceActionsUM

    @Immutable
    data class Row(
        val isLoading: Boolean,
        val isEnabled: Boolean,
        val onClick: () -> Unit,
        val onLongClick: (() -> Unit)? = null,
    )
}