package com.tangem.feature.swap.model

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokensDataStateExpress
import java.math.BigDecimal

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

    // Amount from input
    val amount: String? = null,
    val reduceBalanceBy: BigDecimal = BigDecimal.ZERO,
    val swapDataModel: SwapDataModel? = null,
    val tokensDataState: TokensDataStateExpress? = null,
) {

    fun getCurrentLoadedSwapState(): SwapState.QuotesLoadedState? {
        return lastLoadedSwapStates[selectedProvider] as? SwapState.QuotesLoadedState
    }
}