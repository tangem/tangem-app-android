package com.tangem.features.swap.v2.api.choosetoken

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Triggers swap token selection
 */
interface SwapChooseTokenNetworkTrigger {

    suspend fun trigger(cryptoCurrency: CryptoCurrency)
}