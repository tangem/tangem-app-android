package com.tangem.tap

import androidx.hilt.work.HiltWorkerFactory
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.lib.auth.session.DeviceRegistrar
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import com.tangem.tap.common.analytics.AnalyticsHandlersInitializer
import com.tangem.tap.common.analytics.handlers.BlockchainExceptionHandler
import com.tangem.tap.common.analytics.handlers.opentelemetry.OpenTelemetryMetricsHolder
import com.tangem.tap.common.log.TangemLoggingInitializer
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
internal interface ApplicationEntryPoint {

    fun getFeatureTogglesManager(): FeatureTogglesManager

    fun getExcludedBlockchainsManager(): ExcludedBlockchainsManager

    fun getGetAppThemeModeUseCase(): GetAppThemeModeUseCase

    fun getTangemLoggingInitializer(): TangemLoggingInitializer

    fun getBlockchainExceptionHandler(): BlockchainExceptionHandler

    fun getWorkerFactory(): HiltWorkerFactory

    fun getApiConfigsManager(): ApiConfigsManager

    fun getWcInitializeUseCase(): WcInitializeUseCase

    fun getABTestsManager(): ABTestsManager

    fun getDeviceKeyManager(): DeviceKeyManager

    fun getDeviceRegistrar(): DeviceRegistrar

    fun getWalletRegistrationLauncher(): WalletRegistrationLauncher

    fun getUserWalletsListRepository(): UserWalletsListRepository

    fun getOpenTelemetryMetricsHolder(): OpenTelemetryMetricsHolder

    fun getAnalyticsEventHandler(): AnalyticsEventHandler

    fun getAnalyticsHandlersInitializer(): AnalyticsHandlersInitializer

    fun getForegroundActivityObserver(): ForegroundActivityObserver
}