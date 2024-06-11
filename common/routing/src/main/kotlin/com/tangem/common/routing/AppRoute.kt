package com.tangem.common.routing

import android.os.Bundle
import com.tangem.common.routing.bundle.RouteBundleParams
import com.tangem.common.routing.bundle.bundle
import com.tangem.common.routing.entity.SerializableIntent
import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
sealed class AppRoute(val path: String) : Route, RouteBundleParams {

    override fun getBundle(): Bundle = bundle(serializer())

    @Serializable
    data object Initial : AppRoute(path = "/initial")

    @Serializable
    data object Home : AppRoute(path = "/home")

    @Serializable
    data class Welcome(
        val intent: SerializableIntent? = null,
    ) : AppRoute(path = "/welcome") {

        companion object {
            const val INITIAL_INTENT_KEY = "intent"
        }
    }

    @Serializable
    data object Disclaimer : AppRoute(path = "/disclaimer")

    @Serializable
    data object OnboardingNote : AppRoute(path = "/onboarding/note")

    @Serializable
    data class OnboardingWallet(
        val canSkipBackup: Boolean = true,
    ) : AppRoute(path = "/onboarding/wallet${if (canSkipBackup) "/skippable" else ""}") {

        companion object {
            const val CAN_SKIP_BACKUP_KEY = "canSkipBackup"
        }
    }

    @Serializable
    data object OnboardingTwins : AppRoute(path = "/onboarding/twins")

    @Serializable
    data object OnboardingOther : AppRoute(path = "/onboarding/other")

    @Serializable
    data object Wallet : AppRoute(path = "/wallet")

    @Serializable
    data class CurrencyDetails(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
    ) : AppRoute(path = "/currency_details/${userWalletId.stringValue}/${currency.id.value}") {

        companion object {
            const val USER_WALLET_ID_KEY = "userWalletId"
            const val CRYPTO_CURRENCY_KEY = "currency"
        }
    }

    @Serializable
    data class Send(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val transactionId: String? = null,
        val amount: String? = null,
        val tag: String? = null,
        val destinationAddress: String? = null,
    ) : AppRoute(
        path = "/send/${userWalletId.stringValue}/${currency.id.value}?" +
            "&$transactionId" +
            "&$amount" +
            "&$tag" +
            "&$destinationAddress",
    ) {
        companion object {
            const val USER_WALLET_ID_KEY = "userWalletId"
            const val CRYPTO_CURRENCY_KEY = "currency"
            const val TRANSACTION_ID_KEY = "transactionId"
            const val AMOUNT_KEY = "amount"
            const val TAG_KEY = "tag"
            const val DESTINATION_ADDRESS_KEY = "destinationAddress"
        }
    }

    @Serializable
    data class Details(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/details/${userWalletId.stringValue}")

    @Serializable
    data object DetailsSecurity : AppRoute(path = "/details/security")

    @Serializable
    data object CardSettings : AppRoute(path = "/card_settings")

    @Serializable
    data object AppSettings : AppRoute(path = "/app_settings")

    @Serializable
    data object ResetToFactory : AppRoute(path = "/reset_to_factory")

    @Serializable
    data object AccessCodeRecovery : AppRoute(path = "/access_code_recovery")

    @Serializable
    data object ManageTokens : AppRoute(path = "/manage_tokens")

    @Serializable
    data object AddCustomToken : AppRoute(path = "/add_custom_token")

    @Serializable
    data object WalletConnectSessions : AppRoute(path = "/wallet_connect_sessions")

    @Serializable
    data class QrScanning(
        val source: SourceType,
        val networkName: String? = null,
    ) : AppRoute(path = "/qr_scanning") {

        companion object {
            const val SOURCE_KEY = "source"
            const val NETWORK_KEY = "networkName"
        }
    }

    @Serializable
    data object ReferralProgram : AppRoute(path = "/referral_program")

    @Serializable
    data class Swap(
        val currency: CryptoCurrency,
    ) : AppRoute(path = "/swap") {

        companion object {
            const val CURRENCY_BUNDLE_KEY = "currency"
        }
    }

    @Serializable
    data object TesterMenu : AppRoute(path = "/tester_menu")

    @Serializable
    data object SaveWallet : AppRoute(path = "/save_wallet")

    @Serializable
    data object AppCurrencySelector : AppRoute(path = "/app_currency_selector")

    @Serializable
    data object ModalNotification : AppRoute(path = "/modal_notification")

    @Serializable
    data object Staking : AppRoute(path = "/staking")
}