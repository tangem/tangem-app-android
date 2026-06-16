package com.tangem.feature.swap.model

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapState
import java.math.BigDecimal

typealias SuccessLoadedSwapData = Map<SwapProvider, SwapState.QuotesLoadedState>

data class SwapProcessDataState(
    // Initial network id
    val fromSwapCurrencyStatus: SwapCurrencyStatus? = null,
    val toSwapCurrencyStatus: SwapCurrencyStatus? = null,

    val feePaidCryptoCurrency: CryptoCurrencyStatus? = null,

    // swap info
    val pairs: List<SwapPairLeast> = emptyList(),

    val selectedPairProviders: List<SwapProvider> = emptyList(),
    val selectedProvider: SwapProvider? = null,
    val lastLoadedSwapStates: Map<SwapProvider, SwapState> = emptyMap(),
    val currentTransferState: SwapState.Transfer? = null,

    // Amount from input
    val amount: String? = null,
    val reduceBalanceBy: BigDecimal = BigDecimal.ZERO,
) {

    fun getCurrentLoadedSwapState(): SwapState.QuotesLoadedState? {
        return lastLoadedSwapStates[selectedProvider] as? SwapState.QuotesLoadedState
    }

    fun getLastLoadedSuccessStates(): SuccessLoadedSwapData {
        return lastLoadedSwapStates.filter { entry -> entry.value is SwapState.QuotesLoadedState }
            .mapValues { entry -> entry.value as SwapState.QuotesLoadedState }
    }
}

internal fun Map<SwapProvider, SwapState>.getLastLoadedSuccessStates(): SuccessLoadedSwapData {
    return this.filter { entry -> entry.value is SwapState.QuotesLoadedState }
        .mapValues { entry -> entry.value as SwapState.QuotesLoadedState }
}

internal fun Map<SwapProvider, SwapState>.consideredProvidersStates(): Map<SwapProvider, SwapState> {
    fun isUserResolvableError(swapState: SwapState): Boolean {
        return swapState is SwapState.SwapError &&
            (
                swapState.error is ExpressDataError.ExchangeTooSmallAmountError ||
                    swapState.error is ExpressDataError.ExchangeTooBigAmountError
                )
    }

    return this.filter { entry ->
        entry.value is SwapState.QuotesLoadedState || isUserResolvableError(entry.value)
    }
}