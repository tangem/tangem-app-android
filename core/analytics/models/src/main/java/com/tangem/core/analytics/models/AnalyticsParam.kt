package com.tangem.core.analytics.models

sealed class AnalyticsParam {

    sealed class CardBalanceState(val value: String) {
        data object Empty : CardBalanceState("Empty")
        data object Full : CardBalanceState("Full")
        data object CustomToken : CardBalanceState("Custom Token")
        data object BlockchainError : CardBalanceState("Blockchain Error")
        data object NoRate : CardBalanceState("No Rate")
        companion object
    }

    sealed class TokenBalanceState(val value: String) {
        data object Empty : TokenBalanceState("Empty")
        data object Full : TokenBalanceState("Full")
    }

    sealed class RateApp(val value: String) {
        data object Liked : RateApp("Liked")
        data object Disliked : RateApp("Disliked")
        data object Closed : RateApp("Close")
    }

    sealed class OnOffState(val value: String) {
        data object On : OnOffState("On")
        data object Off : OnOffState("Off")
    }

    sealed class OrganizeSortType(val value: String) {
        data object ByBalance : OrganizeSortType("By Balance")
        data object Manually : OrganizeSortType("Manually")
    }

    sealed class UserCode(val value: String) {
        data object AccessCode : UserCode("Access Code")
        data object Passcode : UserCode("Passcode")
    }

    sealed class AccessCodeRecoveryStatus(val value: String) {

        val key: String = "Status"

        data object Enabled : AccessCodeRecoveryStatus("Enabled")
        data object Disabled : AccessCodeRecoveryStatus("Disabled")

        companion object {
            fun from(enabled: Boolean): AccessCodeRecoveryStatus {
                return if (enabled) Enabled else Disabled
            }
        }
    }

    sealed class Error(val value: String) {
        data object App : Error("App Error")
        data object CardSdk : Error("Card Sdk Error")
        data object BlockchainSdk : Error("Blockchain Sdk Error")
    }

    sealed class ScreensSources(val value: String) {
        data object Settings : ScreensSources("Settings")
        data object Main : ScreensSources("Main")
        data object SignIn : ScreensSources("Sign In")
        data object Send : ScreensSources("Send")
        data object Intro : ScreensSources("Introduction")
        data object MyWallets : ScreensSources("My Wallets")
        data object Token : ScreensSources("Token")
        data object Stories : ScreensSources("Stories")
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

        data object WalletConnect : TxSentFrom("WalletConnect")
        data object Sell : TxSentFrom("Sell")
    }

    sealed interface TxData {
        val blockchain: String
        val token: String
        val feeType: FeeType
    }

    sealed class FeeType(val value: String) {
        data object Fixed : FeeType("Fixed")
        data object Min : FeeType("Min")
        data object Normal : FeeType("Normal")
        data object Max : FeeType("Max")
        data object Custom : FeeType("Custom")

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
        data object PrivateKey : WalletCreationType("Private key")
        data object NewSeed : WalletCreationType("New seed")
        data object SeedImport : WalletCreationType("Seed import")
    }

    sealed class WalletType(val value: String) {
        data object MultiCurrency : WalletType(value = "Multicurrency")
        class SingleCurrency(currencyName: String) : WalletType(currencyName)
    }

    companion object Key {
        const val BLOCKCHAIN = "blockchain"
        const val TOKEN = "Token"
        const val SOURCE = "Source"
        const val BALANCE = "Balance"
        const val STATE = "State"
        const val BATCH = "Batch"
        const val TYPE = "Type"
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
        const val DAPP_URL = "DApp Url"
        const val METHOD_NAME = "Method Name"
        const val VALIDATION = "Validation"
        const val BLOCKCHAIN_EXCEPTION_HOST = "exception_host"
        const val BLOCKCHAIN_SELECTED_HOST = "selected_host"
    }
}