package com.tangem.domain.polymarket

import com.tangem.domain.models.currency.CryptoCurrency

interface PolymarketCollateralCurrencyFactory {

    fun create(): CryptoCurrency.Token

    companion object {
        val TOKEN_ID = CryptoCurrency.RawID(value = "usd-coin")
        const val TOKEN_NAME = "USDC"
        const val TOKEN_SYMBOL = "USDC"
    }
}