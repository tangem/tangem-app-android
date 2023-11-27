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
// [REDACTED_TODO_COMMENT]
    }

    override fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onBuyClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onSwapClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onExploreClick() {
// [REDACTED_TODO_COMMENT]
    }
}
