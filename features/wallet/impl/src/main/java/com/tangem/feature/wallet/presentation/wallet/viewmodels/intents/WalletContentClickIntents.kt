package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import javax.inject.Inject

internal interface WalletContentClickIntents {

    fun onBackClick()

    fun onDetailsClick()

    fun onManageTokensClick()

    fun onOrganizeTokensClick()

    fun onTokenItemClick(currency: CryptoCurrency)

    fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onTransactionClick(txHash: String)
}

internal class WalletContentClickIntentsImplementor @Inject constructor() : WalletContentClickIntents {

    override fun onBackClick() {
        // TODO
    }

    override fun onDetailsClick() {
        // TODO
    }

    override fun onManageTokensClick() {
        // TODO
    }

    override fun onOrganizeTokensClick() {
        // TODO
    }

    override fun onTokenItemClick(currency: CryptoCurrency) {
        // TODO
    }

    override fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onTransactionClick(txHash: String) {
        // TODO
    }
}