package com.tangem.tap.di

import com.tangem.datasource.api.moonpay.MoonPayApi
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.scanCard.repository.DefaultScanCardRepository
import com.tangem.tap.network.exchangeServices.DefaultRampManager
import com.tangem.tap.network.exchangeServices.SellService
import com.tangem.tap.network.exchangeServices.moonpay.MoonPayService
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ActivityModule {

    @Provides
    @Singleton
    fun provideScanCardUseCase(
        tangemSdkManager: TangemSdkManager,
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): ScanCardUseCase {
        return ScanCardUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            scanCardRepository = DefaultScanCardRepository(
                tangemSdkManager = tangemSdkManager,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideDefaultRampManager(
        appStateHolder: AppStateHolder,
        expressServiceFetcher: ExpressServiceFetcher,
        currenciesRepository: CurrenciesRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): RampStateManager {
        return DefaultRampManager(
            sellService = Provider { requireNotNull(appStateHolder.sellService) },
            expressServiceFetcher = expressServiceFetcher,
            currenciesRepository = currenciesRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    @DelayedWork
    fun provideActivityDelayedWorkCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideExchangeService(
        environmentConfigStorage: EnvironmentConfigStorage,
        getSelectedWalletUseCase: GetSelectedWalletUseCase,
        moonPayApi: MoonPayApi,
    ): SellService {
        return MoonPayService(
            api = moonPayApi,
            apiKeyProvider = Provider { environmentConfigStorage.getConfigSync().moonPayApiKey },
            secretKeyProvider = Provider { environmentConfigStorage.getConfigSync().moonPayApiSecretKey },
            userWalletProvider = { getSelectedWalletUseCase.sync().getOrNull() },
        )
    }
}