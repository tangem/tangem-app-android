package com.tangem.tap.common.analytics.events

import com.tangem.tap.features.details.redux.SecurityOption

sealed class AnalyticsParam {

    sealed class CurrencyType(val value: String) {
        class Currency(currency: com.tangem.tap.features.wallet.models.Currency) : CurrencyType(currency.currencySymbol)
        class Blockchain(blockchain: com.tangem.blockchain.common.Blockchain) : CurrencyType(blockchain.currency)
        class Token(token: com.tangem.blockchain.common.Token) : CurrencyType(token.symbol)
        class FiatCurrency(
            fiatCurrency: com.tangem.tap.common.entities.FiatCurrency,
        ) : CurrencyType(fiatCurrency.code)

        class Amount(amount: com.tangem.blockchain.common.Amount) : CurrencyType(amount.currencySymbol)
    }

    // MultiCurrency or CurrencyType
    sealed class CardCurrency(val value: String) {
        object MultiCurrency : CardCurrency("Multicurrency")
        class SingleCurrency(type: CurrencyType) : CardCurrency(type.value)
    }

    sealed class CardBalanceState(val value: String) {
        object Empty : CardBalanceState("Empty")
        object Full : CardBalanceState("Full")
        companion object
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

        companion object {
            fun from(option: SecurityOption): SecurityMode = when (option) {
                SecurityOption.AccessCode -> AccessCode
                SecurityOption.PassCode -> Passcode
                SecurityOption.LongTap -> LongTap
            }
        }
    }

    sealed class Error(val value: String) {
        object App : Error("App Error")
        object CardSdk : Error("Card Sdk Error")
        object BlockchainSdk : Error("Blockchain Sdk Error")
    }

    sealed class ScannedFrom(val value: String) {
        object Introduction : ScannedFrom("Introduction")
        object Main : ScannedFrom("Main")
        object SignIn : ScannedFrom("Sign In")
        object MyWallets : ScannedFrom("My Wallets")
    }

    sealed class TxSentFrom(val value: String) {
        object Send : TxSentFrom("Send")
        object Swap : TxSentFrom("Swap")
        object WalletConnect : TxSentFrom("WalletConnect")
        object Sell : TxSentFrom("Sell")
    }

    companion object Key {
        const val Source = "Source"
        const val Batch = "Batch"
        const val ProductType = "Product Type"
        const val Firmware = "Firmware"
        const val Currency = "Currency"
        const val ErrorDescription = "Error Description"
        const val ErrorCode = "Error Code"
        const val ErrorKey = "Error Key"
    }
}
