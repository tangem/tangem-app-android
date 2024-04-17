package com.tangem.core.analytics.models

sealed class AnalyticsParam {

    sealed class CardBalanceState(val value: String) {
        object Empty : CardBalanceState("Empty")
        object Full : CardBalanceState("Full")
        object CustomToken : CardBalanceState("Custom Token")
        object BlockchainError : CardBalanceState("Blockchain Error")
        object NoRate : CardBalanceState("No Rate")
        companion object
    }

    sealed class TokenBalanceState(val value: String) {
        object Empty : TokenBalanceState("Empty")
        object Full : TokenBalanceState("Full")
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

    sealed class OrganizeSortType(val value: String) {
        object ByBalance : OrganizeSortType("By Balance")
        object Manually : OrganizeSortType("Manually")
    }

    sealed class UserCode(val value: String) {
        object AccessCode : UserCode("Access Code")
        object Passcode : UserCode("Passcode")
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

    sealed class ScreensSources(val value: String) {
        data object Settings : ScreensSources("Settings")
        data object Main : ScreensSources("Main")
        data object SignIn : ScreensSources("Sign In")
        data object Send : ScreensSources("Send")
        data object Intro : ScreensSources("Introduction")
        data object MyWallets : ScreensSources("My Wallets")
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

    sealed class WalletType(val value: String) {
        object MultiCurrency : WalletType(value = "Multicurrency")
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