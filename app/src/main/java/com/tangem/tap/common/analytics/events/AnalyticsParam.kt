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
        object CustomToken : CardBalanceState("Custom token")
        object BlockchainError : CardBalanceState("Blockchain error")
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

    sealed class AccessCodeRecoveryStatus(val value: String) {

        val key: String = "Status"

        object Enabled : AccessCodeRecoveryStatus("Enabled")
        object Disabled : AccessCodeRecoveryStatus("Disabled")

        companion object {
            fun from(enabled: Boolean): AccessCodeRecoveryStatus {
                return if (enabled) Enabled else Disabled
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
        data class Send(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
        ) : TxSentFrom("Send"), TxData

        data class Swap(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
        ) : TxSentFrom("Swap"), TxData

        data class Approve(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
            val permissionType: String,
        ) : TxSentFrom("Approve"), TxData

        object WalletConnect : TxSentFrom("WalletConnect")
        object Sell : TxSentFrom("Sell")
    }

    sealed interface TxData {
        val blockchain: String
        val token: String
        val feeType: FeeType
    }

    sealed class FeeType(val value: String) {
        object Fixed : FeeType("Fixed")
        object Min : FeeType("Min")
        object Normal : FeeType("Normal")
        object Max : FeeType("Max")

        companion object {
            fun fromString(feeType: String): FeeType {
                return when (feeType) {
                    Min.value -> Min
                    Normal.value -> Normal
                    Max.value -> Max
                    Fixed.value -> Fixed
                    else -> Fixed
                }
            }
        }
    }

    sealed class WalletCreationType(val value: String) {
        object PrivateKey : WalletCreationType("Private key")
        object NewSeed : WalletCreationType("New seed")
        object SeedImport : WalletCreationType("Seed import")
    }

    companion object Key {
        const val BLOCKCHAIN = "blockchain"
        const val TOKEN = "Token"
        const val SOURCE = "Source"
        const val BALANCE = "Balance"
        const val BATCH = "Batch"
        const val FEE_TYPE = "Fee Type"
        const val PERMISSION_TYPE = "Permission Type"
        const val PRODUCT_TYPE = "Product Type"
        const val FIRMWARE = "Firmware"
        const val CURRENCY = "Currency"
        const val ERROR_DESCRIPTION = "Error Description"
        const val ERROR_CODE = "Error Code"
        const val ERROR_KEY = "Error Key"
        const val CREATION_TYPE = "Creation type"
        const val DAPP_NAME = "DApp Name"
    }
}
