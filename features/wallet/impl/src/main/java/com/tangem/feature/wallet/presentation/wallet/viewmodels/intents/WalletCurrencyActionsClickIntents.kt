package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import javax.inject.Inject

interface WalletCurrencyActionsClickIntents {

    fun onSendClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onBuyClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onSwapClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onExploreClick()
}

internal class WalletCurrencyActionsClickIntentsImplementor @Inject constructor() : WalletCurrencyActionsClickIntents {

    override fun onSendClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onBuyClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onSwapClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // TODO
    }

    override fun onExploreClick() {
        // TODO
    }
}