package com.tangem.tap

import androidx.hilt.work.HiltWorkerFactory
import com.tangem.TangemSdkLogger
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.core.analytics.paramsinterceptor.SendTransactionSignerInfoInterceptor
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.tap.common.analytics.handlers.BlockchainExceptionHandler
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerClient
import com.tangem.tap.common.log.TangemAppLoggerInitializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApplicationEntryPoint {

    fun getEnvironmentConfig(): EnvironmentConfig

    fun getFeatureTogglesManager(): FeatureTogglesManager

    fun getExcludedBlockchainsManager(): ExcludedBlockchainsManager

    fun getGetAppThemeModeUseCase(): GetAppThemeModeUseCase

    fun getWalletsRepository(): WalletsRepository

    fun getOneTimeEventFilter(): OneTimeEventFilter

    fun getTangemSdkLogger(): TangemSdkLogger

    fun getTangemAppLogger(): TangemAppLoggerInitializer

    fun getAppLogsStore(): AppLogsStore

    fun getBlockchainExceptionHandler(): BlockchainExceptionHandler

    fun getWorkerFactory(): HiltWorkerFactory

    fun getApiConfigsManager(): ApiConfigsManager

    fun getWcInitializeUseCase(): WcInitializeUseCase

    fun getABTestsManager(): ABTestsManager

    fun getAppsFlyerClientFactory(): AppsFlyerClient.Factory

    fun getSendTransactionSignerInfoInterceptor(): SendTransactionSignerInfoInterceptor
}