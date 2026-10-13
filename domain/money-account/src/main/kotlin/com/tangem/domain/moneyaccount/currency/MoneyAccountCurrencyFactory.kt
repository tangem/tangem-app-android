package com.tangem.domain.moneyaccount.currency

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface MoneyAccountCurrencyFactory {

    /**
     * Builds the money account token (USDC on Polygon) bound to the hidden money account derivation
     * of the wallet identified by [userWalletId].
     *
     * @throws IllegalStateException if no wallet with [userWalletId] is currently loaded.
     */
    fun create(userWalletId: UserWalletId): CryptoCurrency.Token
}