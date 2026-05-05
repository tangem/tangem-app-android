package com.tangem.core.analytics.models

import com.tangem.core.analytics.models.AnalyticsParam.Key.REFERRAL
import com.tangem.core.analytics.models.AnalyticsParam.Key.REFERRAL_ID
import kotlinx.serialization.Serializable

const val IS_NOT_HTTP_ERROR = "Is not http error"

sealed class AnalyticsParam {

    sealed class CardBalanceState(val value: String) {
        data object Empty : CardBalanceState("Empty")
        data object Full : CardBalanceState("Full")
        data object CustomToken : CardBalanceState("Custom Token")
        data object BlockchainError : CardBalanceState("Blockchain Error")
        data object NoRate : CardBalanceState("No Rate")
        companion object
    }

    sealed class RateApp(val value: String) {
        data object Liked : RateApp("Liked")
        data object Disliked : RateApp("Disliked")
        data object Closed : RateApp("Close")
    }

    enum class OnOffState(val value: String) {
        On("On"),
        Off("Off"),
        ;

        companion object {

            fun from(enabled: Boolean): String {
                val state = if (enabled) On else Off

                return state.value
            }
        }
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

    @Serializable
    enum class ScreensSources(val value: String) {
        Settings("Settings"),
        Main("Main"),
        SignIn("Sign In"),
        Send("Send"),
        Intro("Introduction"),
        MyWallets("My Wallets"),
        Token("Token"),
        Stories("Stories"),
        Buy("Buy"),
        Swap("Swap"),
        Sell("Sell"),
        Backup("Backup"),
        Onboarding("Onboarding"),
        LongTap("Long Tap"),
        Market("Market"),
        Markets("Markets"),
        MarketPulse("Market Pulse"),
        TangemPay("Tangem Pay"),
        WalletSettings("Wallet Settings"),
        Upgrade("Upgrade"),
        HardwareWallet("Hardware Wallet"),
        ImportWallet("Import Wallet"),
        CreateWalletIntro("Create Wallet Intro"),
        AddNewWallet("Add New Wallet"),
        AddNew("Add New"),
        CreateWallet("Create Wallet"),
        NewsList("News List"),
        NewsLink("News Link"),
        NewsPage("News Page"),
        Portfolio("Portfolio"),
        Staking("Staking"),
        Earn("Earn"),
    }

    sealed class TxSentFrom(val value: String) {
        data class Send(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
            override val feeToken: String,
        ) : TxSentFrom("Send"), TxData

        data class Swap(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
            override val feeToken: String,
        ) : TxSentFrom("Swap"), TxData

        data class Staking(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
            override val feeToken: String,
        ) : TxSentFrom("Staking"), TxData

        data class Approve(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
            override val feeToken: String,
            val permissionType: String,
        ) : TxSentFrom("Approve"), TxData

        data class WalletConnect(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType?,
            override val feeToken: String,
        ) : TxSentFrom("WalletConnect"), TxData

        data object Sell : TxSentFrom("Sell")

        data class NFT(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
            override val feeToken: String,
        ) : TxSentFrom("NFT"), TxData

        data class SendWithSwap(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
            override val feeToken: String,
        ) : TxSentFrom("Send&Swap"), TxData

        data class Earning(
            override val blockchain: String,
            override val token: String,
            override val feeType: FeeType,
            override val feeToken: String,
        ) : TxSentFrom("Earning"), TxData
    }

    sealed interface TxData {
        val blockchain: String
        val token: String
        val feeToken: String
        val feeType: FeeType?
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
        data object PrivateKey : WalletCreationType(value = "Private Key")
        data object NewSeed : WalletCreationType(value = "New Seed")
        data object SeedImport : WalletCreationType(value = "Seed Import")
    }

    sealed class WalletType(val value: String) {
        data object MultiCurrency : WalletType(value = "Multicurrency")
        class SingleCurrency(currencyName: String) : WalletType(currencyName)
    }

    enum class Validation(val value: String) {

        OK(value = "Ok"),
        ERROR(value = "Error"),
        ;

        companion object {

            fun from(isValid: Boolean): String {
                val status = if (isValid) OK else ERROR

                return status.value
            }
        }
    }

    enum class Status(val value: String) {
        Success(value = "Success"),
        Error(value = "Error"),
        Pending(value = "Pending"),
    }

    enum class EmptyFull(val value: String) {
        Empty("Empty"),
        Full("Full"),
    }

    enum class ProductType(val value: String) {
        Note("Note"),
        Twins("Twins"),
        Wallet("Wallet"),
        Start2Coin("Start2Coin"),
        Wallet2("Wallet 2.0"),
        Ring("Ring"),
        Visa("VISA"),
        MobileWallet("Mobile Wallet"),
    }

    enum class SignInType(val value: String) {
        Card("Card"),
        Biometric("Biometric"),
        NoSecurity("No Security"),
        AccessCode("Access Code"),
    }

    companion object Key {
        const val BLOCKCHAIN = "Blockchain"
        const val TOKEN_PARAM = "Token"
        const val SOURCE = "Source"
        const val BALANCE = "Balance"
        const val TOKENS_COUNT = "Tokens Count"
        const val STATE = "State"
        const val BATCH = "Batch"
        const val TYPE = "Type"
        const val FEE_TYPE = "Fee Type"
        const val WALLET_FORM = "WalletForm"
        const val PERMISSION_TYPE = "Permission Type"
        const val PRODUCT_TYPE = "Product Type"
        const val FIRMWARE = "Firmware"
        const val CURRENCY = "Currency"
        const val ERROR_DESCRIPTION = "Error Description"
        const val ERROR_CODE = "Error Code"
        const val ERROR_KEY = "Error Key"
        const val ERROR_TYPE = "Error Type"
        const val ERROR_MESSAGE = "Error Message"
        const val CREATION_TYPE = "Creation type"
        const val DAPP_NAME = "DApp Name"
        const val DAPP_URL = "DApp Url"
        const val METHOD_NAME = "Method Name"
        const val VALIDATION = "Validation"
        const val BLOCKCHAIN_EXCEPTION_HOST = "exception_host"
        const val BLOCKCHAIN_SELECTED_HOST = "selected_host"
        const val INPUT = "Input"
        const val COUNT = "Count"
        const val DERIVATION = "Derivation"
        const val STATUS = "Status"
        const val PROVIDER = "Provider"
        const val PLACE = "Place"
        const val RESIDENCE = "Residence"
        const val PAYMENT_METHOD = "Payment Method"
        const val WATCHED = "Watched"
        const val ACTION = "Action"
        const val COLLECTIONS = "Collections"
        const val NFT = "Nft"
        const val NONCE = "Nonce"
        const val STANDARD = "Standard"
        const val NO_COLLECTION = "No collection"
        const val EMULATION_STATUS = "Emulation Status"
        const val SEND_TOKEN = "Send Token"
        const val RECEIVE_TOKEN = "Receive Token"
        const val SEND_BLOCKCHAIN = "Send Blockchain"
        const val RECEIVE_BLOCKCHAIN = "Receive Blockchain"
        const val CHOSEN_TOKEN = "Token Chosen"
        const val ENS = "ENS"
        const val ENS_ADDRESS = "ENS Address"
        const val ACCOUNT_DERIVATION_FROM = "Account Derivation (from)"
        const val ACCOUNT_DERIVATION_TO = "Account Derivation (to)"
        const val FEE_TOKEN = "Fee Token"
        const val ACCOUNT_DERIVATION = "Account Derivation"
        const val REFERRAL = "Referral"
        const val REFERRAL_ID = "Referral_ID"
        const val SEARCHED = "Searched"
        const val RATE_TYPE = "Rate Type"
        const val SIGN_IN_TYPE = "Sign in type"
        const val WALLETS_COUNT = "Wallets Count"
        const val WALLET_TYPE = "Wallet Type"
        const val BACKUPED = "Backuped"
        const val MEMO = "Memo"
    }
}

fun getReferralParams(referralId: String?): List<Pair<String, String>> = listOf(
    REFERRAL to (!referralId.isNullOrBlank()).toString().replaceFirstChar(Char::titlecase),
    REFERRAL_ID to (referralId ?: "Empty"),
)