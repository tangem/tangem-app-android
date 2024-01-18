package com.tangem.tap.common.analytics.events

import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.tap.features.details.redux.SecurityOption

sealed class AnalyticsParam {

    sealed class CurrencyType(val value: String) {
        class Currency(currency: com.tangem.tap.domain.model.Currency) : CurrencyType(currency.currencySymbol)
        class Blockchain(blockchain: com.tangem.blockchain.common.Blockchain) : CurrencyType(blockchain.currency)
        class Token(token: com.tangem.blockchain.common.Token) : CurrencyType(token.symbol)
        class Amount(amount: com.tangem.blockchain.common.Amount) : CurrencyType(amount.currencySymbol)
    }

    sealed class CardBalanceState(val value: String) {
        object Empty : CardBalanceState("Empty")
        object Full : CardBalanceState("Full")
        companion object
    }

    sealed class RateApp(val value: String) {
        object Liked : RateApp("Liked")
        object Closed : RateApp("Close")
    }

    sealed class OnOffState(val value: String) {

        object On : OnOffState("On")
        object Off : OnOffState("Off")

        companion object {

            operator fun invoke(value: Boolean): OnOffState = if (value) On else Off
        }
    }

    sealed class UserCode(val value: String) {
        object AccessCode : UserCode("Access Code")
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

    sealed class WalletCreationType(val value: String) {
        object PrivateKey : WalletCreationType(value = "Private Key")
        object NewSeed : WalletCreationType(value = "New Seed")
        object SeedImport : WalletCreationType(value = "Seed Import")
    }

    sealed class AppTheme(val value: String) {
        object System : AppTheme("System")
        object Dark : AppTheme("Dark")
        object Light : AppTheme("Light")

        companion object {
            fun fromAppThemeMode(mode: AppThemeMode): AppTheme {
                return when (mode) {
                    AppThemeMode.FORCE_DARK -> Dark
                    AppThemeMode.FORCE_LIGHT -> Light
                    AppThemeMode.FOLLOW_SYSTEM -> System
                }
            }
        }
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
        const val CREATION_TYPE = "Creation Type"
        const val SEED_PHRASE_LENGTH = "Seed Phrase Length"
        const val DAPP_NAME = "DApp Name"
        const val DAPP_URL = "DApp Url"
        const val METHOD_NAME = "Method Name"
        const val VALIDATION = "Validation"
        const val BLOCKCHAIN_EXCEPTION_HOST = "exception_host"
        const val BLOCKCHAIN_SELECTED_HOST = "selected_host"
    }
}