package com.tangem.tap.common.analytics.events

sealed class AnalyticsParam {

    sealed class CurrencyType(val value: String) {
        class Currency(currency: com.tangem.tap.features.wallet.models.Currency) : CurrencyType(currency.currencySymbol)
        class Blockchain(blockchain: com.tangem.blockchain.common.Blockchain) : CurrencyType(blockchain.currency)
        class Token(token: com.tangem.blockchain.common.Token) : CurrencyType(token.symbol)
    }

    // Multicurrency or CurrencyType
    sealed class CardCurrency(val value: String) {
        object MultiCurrency : CardCurrency("Multicurrency")
        class SingleCurrency(type: CurrencyType) : CardCurrency(type.value)
    }

    sealed class CardState(val value: String) {
        object Empty : CardState("Empty")
        object Full : CardState("Full")
    }

    sealed class RateApp(val value: String) {
        object Liked : RateApp("Liked")
        object Disliked : RateApp("Disliked")
        object Closed : RateApp("Close")
    }

    sealed class OnOffState(val value: String) {
        object On : OnOffState("On")
        object Off : OnOffState("Off")
    }

    sealed class UserCode(val value: String) {
        object AccessCode : UserCode("Access Code")
        object Passcode : UserCode("Passcode")
    }

    sealed class SecurityMode(val value: String) {
        object AccessCode : SecurityMode("Access Code")
        object Passcode : SecurityMode("Passcode")
        object LongTap : SecurityMode("Long Tap")
    }

    companion object Key {
        const val Blockchain = "blockchain"
        const val BatchId = "batch_id"
        const val Firmware = "firmware"
    }
}