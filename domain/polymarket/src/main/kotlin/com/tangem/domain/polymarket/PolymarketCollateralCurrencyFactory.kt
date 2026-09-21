package com.tangem.domain.polymarket

import com.tangem.domain.models.currency.CryptoCurrency

interface PolymarketCollateralCurrencyFactory {

    /**
     * @return the exchange's settlement token, in which the trading balance is denominated and every order is
     *  paid; never the token a user sends to top the account up
     */
    fun create(): CryptoCurrency.Token

    /**
     * @return the token a deposit is made in — native USDC on Polygon, which the bridge accepts and every swap
     *  provider routes
     */
    fun createDepositTarget(): CryptoCurrency.Token

    companion object {
        const val COLLATERAL_NAME = "Polymarket USD"
        const val COLLATERAL_SYMBOL = "pUSD"
        const val DEPOSIT_NAME = "USDC"
        const val DEPOSIT_SYMBOL = "USDC"
    }
}