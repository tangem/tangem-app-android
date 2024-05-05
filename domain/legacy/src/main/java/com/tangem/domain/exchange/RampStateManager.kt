package com.tangem.domain.exchange

import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Manager that holds info about available actions as Sell and Buy
 */
interface RampStateManager {

    fun availableForBuy(cryptoCurrency: CryptoCurrency): Boolean

    fun availableForSell(cryptoCurrency: CryptoCurrency): Boolean
}