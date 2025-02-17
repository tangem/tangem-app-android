package com.tangem.tap.proxy.redux

import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.data.card.TransactionSignerFactory
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.config.issuers.IssuersConfigStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.features.onramp.OnrampFeatureToggles
import com.tangem.features.pushnotifications.api.navigation.PushNotificationsRouter
import com.tangem.tap.domain.scanCard.CardScanningFeatureToggles
import com.tangem.tap.domain.walletconnect2.domain.LegacyWalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.proxy.AppStateHolder
import org.rekotlin.StateType

data class DaggerGraphState(
    val networkConnectionManager: NetworkConnectionManager? = null,
    val cardScanningFeatureToggles: CardScanningFeatureToggles? = null,
    val scanCardUseCase: ScanCardUseCase? = null,
    val walletConnectRepository: LegacyWalletConnectRepository? = null,
    val walletConnectInteractor: WalletConnectInteractor? = null,
    val scanCardProcessor: ScanCardProcessor? = null,
    val cardSdkConfigRepository: CardSdkConfigRepository? = null,
    val appCurrencyRepository: AppCurrencyRepository? = null,
    val walletManagersFacade: WalletManagersFacade? = null,
    val appStateHolder: AppStateHolder? = null,
    val appThemeModeRepository: AppThemeModeRepository? = null,
    val balanceHidingRepository: BalanceHidingRepository? = null,
    val walletsRepository: WalletsRepository? = null,
    val generalUserWalletsListManager: UserWalletsListManager? = null,
    val wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase? = null,
    val saveTwinsOnboardingShownUseCase: SaveTwinsOnboardingShownUseCase? = null,
    val generateWalletNameUseCase: GenerateWalletNameUseCase? = null,
    val cardRepository: CardRepository? = null,
    val settingsRepository: SettingsRepository? = null,
    val blockchainSDKFactory: BlockchainSDKFactory? = null,
    val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase? = null,
    val getCardInfoUseCase: GetCardInfoUseCase? = null,
    val issuersConfigStorage: IssuersConfigStorage? = null,
    val urlOpener: UrlOpener? = null,
    val shareManager: ShareManager? = null,
    val appRouter: AppRouter? = null,
    val pushNotificationsRouter: PushNotificationsRouter? = null,
    val transactionSignerFactory: TransactionSignerFactory? = null,
    val getUserCountryUseCase: GetUserCountryUseCase? = null,
    val onrampFeatureToggles: OnrampFeatureToggles? = null,
    val environmentConfigStorage: EnvironmentConfigStorage? = null,
    val onboardingV2FeatureToggles: OnboardingV2FeatureToggles? = null,
    val onboardingRepository: OnboardingRepository? = null,
    val excludedBlockchains: ExcludedBlockchains? = null,
    val appPreferencesStore: AppPreferencesStore? = null,
    val clipboardManager: ClipboardManager? = null,
    val settingsManager: SettingsManager? = null,
    val uiMessageSender: UiMessageSender? = null,
) : StateType