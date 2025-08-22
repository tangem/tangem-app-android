package com.tangem.tap

import androidx.hilt.work.HiltWorkerFactory
import com.tangem.TangemSdkLogger
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.data.card.TransactionSignerFactory
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.config.issuers.IssuersConfigStorage
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
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
import com.tangem.tap.common.analytics.handlers.BlockchainExceptionHandler
import com.tangem.tap.common.log.TangemAppLoggerInitializer
import com.tangem.tap.domain.scanCard.CardScanningFeatureToggles
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.tangem.tap.domain.walletconnect2.domain.LegacyWalletConnectRepository as WalletConnect2Repository

@EntryPoint
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
interface ApplicationEntryPoint {

    fun getEnvironmentConfigStorage(): EnvironmentConfigStorage

    fun getAppStateHolder(): AppStateHolder

    fun getIssuersConfigStorage(): IssuersConfigStorage

    fun getFeatureTogglesManager(): FeatureTogglesManager

    fun getExcludedBlockchainsManager(): ExcludedBlockchainsManager

    fun getNetworkConnectionManager(): NetworkConnectionManager

    fun getCardScanningFeatureToggles(): CardScanningFeatureToggles

    fun getWalletConnect2Repository(): WalletConnect2Repository

    fun getScanCardProcessor(): ScanCardProcessor

    fun getAppCurrencyRepository(): AppCurrencyRepository

    fun getWalletManagersFacade(): WalletManagersFacade

    fun getAppThemeModeRepository(): AppThemeModeRepository

    fun getBalanceHidingRepository(): BalanceHidingRepository

    fun getAppPreferencesStore(): AppPreferencesStore

    fun getGetAppThemeModeUseCase(): GetAppThemeModeUseCase

    fun getWalletsRepository(): WalletsRepository

    fun getOneTimeEventFilter(): OneTimeEventFilter

    fun getGeneralUserWalletsListManager(): UserWalletsListManager

    fun getWasTwinsOnboardingShownUseCase(): WasTwinsOnboardingShownUseCase

    fun getSaveTwinsOnboardingShownUseCase(): SaveTwinsOnboardingShownUseCase

    fun getCardRepository(): CardRepository

    fun getTangemSdkLogger(): TangemSdkLogger

    fun getSettingsRepository(): SettingsRepository

    fun getBlockchainSDKFactory(): BlockchainSDKFactory

    fun getSendFeedbackEmailUseCase(): SendFeedbackEmailUseCase

    fun getGetCardInfoUseCase(): GetCardInfoUseCase

    fun getUrlOpener(): UrlOpener

    fun getShareManager(): ShareManager

    fun getAppRouter(): AppRouter

    fun getTangemAppLogger(): TangemAppLoggerInitializer

    fun getTransactionSignerFactory(): TransactionSignerFactory

    fun getOnboardingV2FeatureToggles(): OnboardingV2FeatureToggles

    fun getOnboardingRepository(): OnboardingRepository

    fun getCoroutineDispatcherProvider(): CoroutineDispatcherProvider

    fun getExcludedBlockchains(): ExcludedBlockchains

    fun getAppLogsStore(): AppLogsStore

    fun getClipboardManager(): ClipboardManager

    fun getSettingsManager(): SettingsManager

    fun getBlockchainExceptionHandler(): BlockchainExceptionHandler

    @GlobalUiMessageSender
    fun getUiMessageSender(): UiMessageSender

    fun getWorkerFactory(): HiltWorkerFactory

    fun getColdUserWalletBuilderFactory(): ColdUserWalletBuilder.Factory

    fun getApiConfigsManager(): ApiConfigsManager

    fun getUserTokensResponseStore(): UserTokensResponseStore

    fun getUserWalletsListRepository(): UserWalletsListRepository

    fun getTangemHotSdk(): TangemHotSdk

    fun getHotWalletFeatureToggles(): HotWalletFeatureToggles
}