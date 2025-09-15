package com.tangem.domain.card.analytics

internal sealed class AnalyticsParam {

    sealed class CurrencyType(val value: String) {
        class Blockchain(blockchain: com.tangem.blockchain.common.Blockchain) : CurrencyType(blockchain.currency)
        class Token(token: com.tangem.blockchain.common.Token) : CurrencyType(token.symbol)
    }
}