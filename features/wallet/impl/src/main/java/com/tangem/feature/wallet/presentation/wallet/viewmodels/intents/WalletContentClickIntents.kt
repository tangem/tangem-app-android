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
// [REDACTED_TODO_COMMENT]
    }

    override fun onDetailsClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onManageTokensClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onOrganizeTokensClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onTokenItemClick(currency: CryptoCurrency) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onTransactionClick(txHash: String) {
// [REDACTED_TODO_COMMENT]
    }
}
