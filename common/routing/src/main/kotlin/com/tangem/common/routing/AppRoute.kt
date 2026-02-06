@file:Suppress("NullableToStringCall")

package com.tangem.common.routing

import android.annotation.SuppressLint
import android.os.Bundle
import com.tangem.common.routing.bundle.RouteBundleParams
import com.tangem.common.routing.bundle.bundle
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.tokens.model.details.NavigationAction
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
sealed class AppRoute(val path: String) : Route {

    @Serializable
    data object Initial : AppRoute(path = "/initial")

    @Serializable
    data class Home(
        val launchMode: InitScreenLaunchMode = InitScreenLaunchMode.Standard,
    ) : AppRoute(path = "/home")

    @Serializable
    data class Welcome(
        @Deprecated("No longer used, will be removed in future releases")
        val launchMode: InitScreenLaunchMode = InitScreenLaunchMode.Standard,
    ) : AppRoute(path = "/welcome"), RouteBundleParams {

        override fun getBundle(): Bundle = bundle(serializer())
    }

    @Serializable
    data class Disclaimer(
        val isTosAccepted: Boolean,
    ) : AppRoute(path = "/disclaimer${if (isTosAccepted) "/tos_accepted" else ""}")

    @Serializable
    data object Wallet : AppRoute(path = "/wallet")

    @Serializable
    data class CurrencyDetails(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val navigationAction: NavigationAction? = null,
    ) : AppRoute(path = "/currency_details/${userWalletId.stringValue}/${currency.id.value}")

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
    )

    @Serializable
    data class Details(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/details/${userWalletId.stringValue}")

    @Serializable
    data class DetailsSecurity(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/details/security")

    @Serializable
    data class Usedesk(
        val walletMetaInfo: WalletMetaInfo,
    ) : AppRoute(path = "/usedesk/${walletMetaInfo.userWalletId}")

    @Serializable
    data class CardSettings(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/card_settings/${userWalletId.stringValue}")

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
        val hasTangemPay: Boolean,
    ) : AppRoute(
        path = "/reset_to_factory" +
            "/${userWalletId.stringValue}" +
            "/$cardId" +
            "/$isActiveBackupStatus" +
            "/$backupCardsCount",
    )

    @Serializable
    data object AccessCodeRecovery : AppRoute(path = "/access_code_recovery")

    @Serializable
    data class ManageTokens(
        val source: Source,
        val portfolioId: PortfolioId? = null,
    ) : AppRoute(path = "${source.name.lowercase()}/manage_tokens/${portfolioId?.stringValue}") {

        /**
         * Source of launching the screen.
         * ManageTokens screen launched from Onboarding by another route. See `OnboardingRoute.ManageTokens`.
         */
        enum class Source {
            STORIES,
            SETTINGS,
            ACCOUNT,
        }
    }

    data class ChooseManagedTokens(
        val userWalletId: UserWalletId,
        val initialCurrency: CryptoCurrency,
        val selectedCurrency: CryptoCurrency?,
        val source: Source,
        val shouldShowSendViaSwapNotification: Boolean,
        val analyticsCategoryName: String,
    ) : AppRoute(path = "/$source/choose_managed_tokens/$userWalletId/${initialCurrency.id.value}") {
        enum class Source {
            SendViaSwap,
        }
    }

    @Serializable
    data class WalletConnectSessions(val userWalletId: UserWalletId) : AppRoute(path = "/wallet_connect_sessions")

    @Serializable
    data class QrScanning(val source: Source) : AppRoute(path = "/$source/qr_scanning${source.path}") {

        @Serializable
        sealed class Source {
            val path: String
                get() = when (this) {
                    is Send -> "/$networkName"
                    WalletConnect -> ""
                }

            data class Send(val networkName: String) : Source()

            data object WalletConnect : Source()
        }
    }

    @Serializable
    data class ReferralProgram(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/referral_program")

    @Serializable
    data class Swap(
        val currencyFrom: CryptoCurrency,
        val currencyTo: CryptoCurrency? = null,
        val userWalletId: UserWalletId,
        val isInitialReverseOrder: Boolean = false,
        val screenSource: String,
        val tangemPayInput: TangemPayInput? = null,
    ) : AppRoute(
        path = "/swap" +
            "/${currencyFrom.id.value}" +
            "/${currencyTo?.id?.value}" +
            "/${userWalletId.stringValue}" +
            "/$isInitialReverseOrder",
    ) {
        @Serializable
        data class TangemPayInput(
            val cryptoAmount: SerializedBigDecimal,
            val fiatAmount: SerializedBigDecimal,
            val depositAddress: String,
            val isWithdrawal: Boolean,
        )
    }

    @Serializable
    data object AppCurrencySelector : AppRoute(path = "/app_currency_selector")

    @Serializable
    data class Staking(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val integrationId: StakingIntegrationID,
    ) : AppRoute(path = "/staking/${userWalletId.stringValue}/${cryptoCurrency.id.value}/${integrationId.value}")

    @Serializable
    data class PushNotification(
        val source: Source,
    ) : AppRoute(path = "/push_notification") {
        enum class Source {
            Stories,
            Main,
            Onboarding,
        }
    }

    @Serializable
    data class WalletSettings(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/wallet_settings/${userWalletId.stringValue}")

    @Serializable
    data class WalletBackup(
        val userWalletId: UserWalletId,
        val isColdWalletOptionShown: Boolean,
    ) : AppRoute(path = "/wallet_backup/${userWalletId.stringValue}/$isColdWalletOptionShown")

    @Serializable
    data class WalletHardwareBackup(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/wallet_hardware_backup/${userWalletId.stringValue}")

    @Serializable
    data object Markets : AppRoute(path = "/markets")

    @Serializable
    data class MarketsTokenDetails(
        val token: TokenMarketParams,
        val appCurrency: AppCurrency,
        val shouldShowPortfolio: Boolean,
        val analyticsParams: AnalyticsParams? = null,
    ) : AppRoute(path = "/markets_token_details/${token.id}/$shouldShowPortfolio") {

        @Serializable
        data class AnalyticsParams(
            val blockchain: String?,
            val source: String,
        )
    }

    @Serializable
    data class Onramp(
        val source: OnrampSource,
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val shouldLaunchSepa: Boolean = false,
    ) : AppRoute(path = "/onramp/${userWalletId.stringValue}/${currency.symbol}"), RouteBundleParams {
        override fun getBundle(): Bundle = bundle(serializer())
    }

    @Serializable
    data class OnrampSuccess(
        val txId: String,
    ) : AppRoute(path = "/onramp/success/$txId"), RouteBundleParams {
        override fun getBundle(): Bundle = bundle(serializer())
    }

    @Serializable
    data class BuyCrypto(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/buy_crypto/${userWalletId.stringValue}")

    @Serializable
    data class SellCrypto(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/sell_crypto/${userWalletId.stringValue}")

    @Serializable
    data class SwapCrypto(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/swap_crypto/${userWalletId.stringValue}")

    /**
     * Onboarding V2
     * @property scanResponse scan response, determines onboarding route by the product type
     * @property mode (MultiWallet param, doesn't affect other types) onboarding mode
     */
    @Serializable
    data class Onboarding(
        val scanResponse: ScanResponse,
        val mode: Mode = Mode.Onboarding,
    ) : AppRoute(path = "/onboarding_v2/$mode") {

        @Serializable
        sealed class Mode {
            data object Onboarding : Mode() // general Mode
            data object AddBackupWallet1 : Mode() // continue backup process for existing wallet 1
            data object WelcomeOnlyTwin : Mode() // show welcome screen and then navigate to wallet for twins
            data object RecreateWalletTwin : Mode() // reset twins
            data object ContinueFinalize : Mode() // continue finalize process (unfinished backup dialog)
            data class UpgradeHotWallet(val userWalletId: UserWalletId) : Mode() // upgrade hot wallet
        }
    }

    @Serializable
    data class Stories(
        val storyId: String,
        val nextScreen: AppRoute,
        val screenSource: String,
    ) : AppRoute(path = "/stories$storyId")

    @Serializable
    data class NFT(
        val userWalletId: UserWalletId,
        val walletName: String,
    ) : AppRoute(path = "/nft/${userWalletId.stringValue}")

    @Serializable
    data class NFTSend(
        val userWalletId: UserWalletId,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
    ) : AppRoute(path = "/send/nft/${userWalletId.stringValue}/$nftCollectionName/${nftAsset.id}")

    @Serializable
    object CreateWalletSelection : AppRoute(path = "/create_wallet_selection")

    @Serializable
    data class CreateWalletStart(
        val mode: Mode,
    ) : AppRoute(path = "/create_wallet_start") {
        enum class Mode {
            ColdWallet,
            HotWallet,
        }
    }

    @Serializable
    object CreateHardwareWallet : AppRoute(path = "/create_hardware_wallet")

    @Serializable
    data class CreateMobileWallet(
        val source: String,
    ) : AppRoute(path = "/create_mobile_wallet")

    @Serializable
    data class UpgradeWallet(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/upgrade_wallet/${userWalletId.stringValue}")

    @Serializable
    object AddExistingWallet : AppRoute(path = "/add_existing_wallet")

    @Serializable
    data class WalletActivation(
        val userWalletId: UserWalletId,
        val isBackupExists: Boolean,
    ) : AppRoute(path = "/wallet_activation/${userWalletId.stringValue}")

    @Serializable
    data class CreateWalletBackup(
        val userWalletId: UserWalletId,
        val analyticsSource: String,
        val analyticsAction: String,
        val isUpgradeFlow: Boolean = false,
        val shouldSetAccessCode: Boolean = false,
    ) : AppRoute(path = "/create_wallet_backup/${userWalletId.stringValue}")

    @Serializable
    data class UpdateAccessCode(
        val userWalletId: UserWalletId,
        val source: String,
    ) : AppRoute(path = "/update_access_code/${userWalletId.stringValue}")

    @Serializable
    data class ViewPhrase(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/view_seed_phrase/${userWalletId.stringValue}")

    @Serializable
    data class ForgetWallet(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/forget_wallet/${userWalletId.stringValue}")

    @Serializable
    data class SendEntryPoint(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
    ) : AppRoute(
        path = "/send_entry_point/${userWalletId.stringValue}/${currency.id.value}?",
    )

    @Serializable
    data class CreateAccount(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/create_account/${userWalletId.stringValue}")

    @Serializable
    data class EditAccount(
        val account: Account,
    ) : AppRoute(path = "/edit_account/${account.accountId.value}")

    @Serializable
    data class AccountDetails(
        val account: Account,
    ) : AppRoute(path = "/account_details/${account.accountId.value}")

    @Serializable
    data class ArchivedAccountList(
        val userWalletId: UserWalletId,
    ) : AppRoute(path = "/archived_account/${userWalletId.stringValue}")

    @Serializable
    data class TangemPayDetails(
        val userWalletId: UserWalletId,
        val config: TangemPayDetailsConfig,
    ) : AppRoute(path = "/tangem_pay_details/${userWalletId.stringValue}")

    @Serializable
    data class TangemPayOnboarding(
        val mode: Mode,
    ) : AppRoute(path = "/tangem_pay_onboarding/$mode") {

        @Serializable
        sealed class Mode {
            @Serializable
            data class Deeplink(
                val deeplink: String,
            ) : Mode()

            @Serializable
            data class ContinueOnboarding(
                val userWalletId: UserWalletId,
            ) : Mode()

            @Serializable
            data object FromBannerOnMain : Mode()

            @Serializable
            data object FromBannerInSettings : Mode()
        }
    }

    @Serializable
    data class Kyc(val userWalletId: UserWalletId) : AppRoute(path = "/kyc")

    @Serializable
    data class YieldSupplyEntry(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val apy: String,
    ) : AppRoute(path = "/yield_supply_entry/${userWalletId.stringValue}/${cryptoCurrency.symbol}")

    @Serializable
    data class NewsDetails(val newsId: Int) : AppRoute(path = "/news_details/$newsId")
}