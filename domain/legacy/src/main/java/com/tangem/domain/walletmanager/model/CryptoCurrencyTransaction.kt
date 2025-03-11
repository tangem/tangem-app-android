package com.tangem.domain.walletmanager.model

import com.tangem.domain.txhistory.models.TxHistoryItem

// TODO: [REDACTED_JIRA] move to txhistory module
sealed class CryptoCurrencyTransaction {

    abstract val txHistoryItem: TxHistoryItem

    data class Coin(override val txHistoryItem: TxHistoryItem) : CryptoCurrencyTransaction()

    data class Token(
        val tokenId: String?,
        val tokenContractAddress: String,
        override val txHistoryItem: TxHistoryItem,
    ) : CryptoCurrencyTransaction()
}