package com.tangem.common.routing

import android.os.Bundle
import com.tangem.common.routing.bundle.RouteBundleParams
import com.tangem.common.routing.bundle.bundle
import com.tangem.common.routing.entity.SerializableIntent
import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
sealed class AppRoute(val path: String) : Route {

    @Serializable
    data object Initial : AppRoute(path = "/initial")

    @Serializable
    data object Home : AppRoute(path = "/home")

    @Serializable
    data class Welcome(
        val intent: SerializableIntent? = null,
    ) : AppRoute(path = "/welcome"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val INITIAL_INTENT_KEY = "intent"
        }
    }

    @Serializable
    data class Disclaimer(
        val isTosAccepted: Boolean,
    ) : AppRoute(path = "/disclaimer${if (isTosAccepted) "/tos_accepted" else ""}"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val IS_TOS_ACCEPTED_KEY = "isTosAccepted"
        }
    }

    @Serializable
    data object OnboardingNote : AppRoute(path = "/onboarding/note")

    @Serializable
    data class OnboardingWallet(
        val canSkipBackup: Boolean = true,
    ) : AppRoute(path = "/onboarding/wallet${if (canSkipBackup) "/skippable" else ""}"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

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
    ) : AppRoute(path = "/currency_details/${userWalletId.stringValue}/${currency.id.value}"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

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
    ),
        RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

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
    data class DetailsSecurity(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/details/security"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val USER_WALLET_ID_KEY = "userWalletId"
        }
    }

    @Serializable
    data class CardSettings(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/card_settings/${userWalletId.stringValue}"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val USER_WALLET_ID_KEY = "userWalletId"
        }
    }

    @Serializable
    data object AppSettings : AppRoute(path = "/app_settings")

    /**
     * Reset to factory
     *
     * @property userWalletId         user wallet id
     * @property cardId               reset card id
     * @property isActiveBackupStatus reset backup card status
     * @property backupCardsCount     backup cards count
     */
    @Serializable
    data class ResetToFactory(
        val userWalletId: UserWalletId,
        val cardId: String,
        val isActiveBackupStatus: Boolean,
        val backupCardsCount: Int,
    ) : AppRoute(
        path = "/reset_to_factory" +
            "/${userWalletId.stringValue}" +
            "/$cardId" +
            "/$isActiveBackupStatus" +
            "/$backupCardsCount",
    ),
        RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val USER_WALLET_ID = "userWalletId"
            const val CARD_ID = "cardId"
            const val IS_ACTIVE_BACKUP_STATUS = "isActiveBackupStatus"
            const val BACKUP_CARDS_COUNT = "backupCardsCount"
        }
    }

    @Serializable
    data class AccessCodeRecovery(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/access_code_recovery/${userWalletId.stringValue}"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val USER_WALLET_ID_KEY = "userWalletId"
        }
    }

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
    ) : AppRoute(path = "/$source/qr_scanning${if (networkName != null) "/$networkName" else ""}"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val SOURCE_KEY = "source"
            const val NETWORK_KEY = "networkName"
        }
    }

    @Serializable
    data class ReferralProgram(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/referral_program"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val USER_WALLET_ID_KEY = "userWalletId"
        }
    }

    @Serializable
    data class Swap(
        val currency: CryptoCurrency,
    ) : AppRoute(path = "/swap/${currency.id.value}"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

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
    data class Staking(
        val userWalletId: UserWalletId,
        val cryptoCurrencyId: CryptoCurrency.ID,
        val yield: Yield,
    ) : AppRoute(path = "/staking/${userWalletId.stringValue}/${cryptoCurrencyId.value}/${yield.id}"),
        RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())

        companion object {
            const val USER_WALLET_ID_KEY = "userWalletId"
            const val CRYPTO_CURRENCY_ID_KEY = "cryptoCurrencyId"
            const val YIELD_KEY = "yield"
        }
    }

    @Serializable
    data object PushNotification : AppRoute(path = "/push_notification")

    @Serializable
    data class WalletSettings(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/wallet_settings/${userWalletId.stringValue}")
}