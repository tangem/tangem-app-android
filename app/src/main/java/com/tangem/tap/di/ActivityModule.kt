package com.tangem.tap.di

import com.tangem.datasource.exchangeservice.swap.SwapServiceLoader
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.GetNetworkCoinStatusUseCase
import com.tangem.domain.tokens.GetPolkadotCheckHasImmortalUseCase
import com.tangem.domain.tokens.GetPolkadotCheckHasResetUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.PolkadotAccountHealthCheckRepository
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.scanCard.repository.DefaultScanCardRepository
import com.tangem.tap.network.exchangeServices.DefaultRampManager
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
        swapServiceLoader: SwapServiceLoader,
        currenciesRepository: CurrenciesRepository,
        getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
        dispatchers: CoroutineDispatcherProvider,
    ): RampStateManager {
        return DefaultRampManager(
            exchangeService = appStateHolder.exchangeService,
            buyService = Provider { requireNotNull(appStateHolder.buyService) },
            sellService = Provider { requireNotNull(appStateHolder.sellService) },
            swapServiceLoader = swapServiceLoader,
            currenciesRepository = currenciesRepository,
            getNetworkCoinStatusUseCase = getNetworkCoinStatusUseCase,
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
    fun provideGetPolkadotCheckHasResetUseCase(
        polkadotAccountHealthCheckRepository: PolkadotAccountHealthCheckRepository,
    ): GetPolkadotCheckHasResetUseCase {
        return GetPolkadotCheckHasResetUseCase(polkadotAccountHealthCheckRepository)
    }

    @Provides
    @Singleton
    fun provideGetPolkadotCheckHasImmortalUseCase(
        polkadotAccountHealthCheckRepository: PolkadotAccountHealthCheckRepository,
    ): GetPolkadotCheckHasImmortalUseCase {
        return GetPolkadotCheckHasImmortalUseCase(polkadotAccountHealthCheckRepository)
    }
}
