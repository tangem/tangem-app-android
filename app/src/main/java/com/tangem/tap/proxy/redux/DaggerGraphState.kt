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
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.operations.attestation.CardArtworksProvider
import com.tangem.tap.domain.scanCard.CardScanningFeatureToggles
import com.tangem.tap.proxy.AppStateHolder
import org.rekotlin.StateType

data class DaggerGraphState(
    val networkConnectionManager: NetworkConnectionManager? = null,
    val cardScanningFeatureToggles: CardScanningFeatureToggles? = null,
    val scanCardUseCase: ScanCardUseCase? = null,
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
    val cardRepository: CardRepository? = null,
    val settingsRepository: SettingsRepository? = null,
    val blockchainSDKFactory: BlockchainSDKFactory? = null,
    val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase? = null,
    val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase? = null,
    val issuersConfigStorage: IssuersConfigStorage? = null,
    val urlOpener: UrlOpener? = null,
    val shareManager: ShareManager? = null,
    val appRouter: AppRouter? = null,
    val transactionSignerFactory: TransactionSignerFactory? = null,
    val environmentConfigStorage: EnvironmentConfigStorage? = null,
    val onboardingV2FeatureToggles: OnboardingV2FeatureToggles? = null,
    val onboardingRepository: OnboardingRepository? = null,
    val excludedBlockchains: ExcludedBlockchains? = null,
    val appPreferencesStore: AppPreferencesStore? = null,
    val clipboardManager: ClipboardManager? = null,
    val settingsManager: SettingsManager? = null,
    val uiMessageSender: UiMessageSender? = null,
    val cardArworksProvider: CardArtworksProvider? = null,
    val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory? = null,
    val userTokensResponseStore: UserTokensResponseStore? = null,
    val userWalletsListRepository: UserWalletsListRepository? = null,
    val hotWalletFeatureToggles: HotWalletFeatureToggles? = null,
    val tangemHotSdk: TangemHotSdk? = null,
) : StateType