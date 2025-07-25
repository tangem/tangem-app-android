package com.tangem.features.swap.v2.api.choosetoken

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import kotlinx.coroutines.flow.SharedFlow

/**
 * Triggers swap token selection
 */
interface SwapChooseTokenNetworkTrigger {

    suspend fun trigger(swapCurrencies: SwapCurrencies, cryptoCurrency: CryptoCurrency, shouldResetNavigation: Boolean)
}

/**
 * Listens to swap token selection
 */
interface SwapChooseTokenNetworkListener {
    val swapChooseTokenNetworkResultFlow: SharedFlow<SwapChooseTokenTriggerData>
}

data class SwapChooseTokenTriggerData(
    val swapCurrencies: SwapCurrencies,
    val cryptoCurrency: CryptoCurrency,
    val shouldResetNavigation: Boolean,
)