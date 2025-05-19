package com.tangem.domain.walletmanager.model

import com.tangem.domain.models.network.TxInfo

// TODO: [REDACTED_JIRA] move to txhistory module
sealed class CryptoCurrencyTransaction {

    abstract val txInfo: TxInfo

    data class Coin(override val txInfo: TxInfo) : CryptoCurrencyTransaction()

    data class Token(
        val tokenId: String?,
        val tokenContractAddress: String,
        override val txInfo: TxInfo,
    ) : CryptoCurrencyTransaction()
}