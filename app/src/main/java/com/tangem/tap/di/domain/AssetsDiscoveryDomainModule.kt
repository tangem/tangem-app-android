package com.tangem.tap.di.domain

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.assetsdiscovery.repository.AssetsDiscoveryRepository
import com.tangem.domain.assetsdiscovery.usecase.AcknowledgeAssetsDiscoveryCompletionUseCase
import com.tangem.domain.assetsdiscovery.usecase.ObserveAssetsDiscoveryUseCase
import com.tangem.domain.assetsdiscovery.usecase.StartAssetsDiscoveryUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AssetsDiscoveryDomainModule {

    @Provides
    @Singleton
    fun provideObserveAssetsDiscoveryUseCase(
        assetsDiscoveryRepository: AssetsDiscoveryRepository,
    ): ObserveAssetsDiscoveryUseCase {
        return ObserveAssetsDiscoveryUseCase(
            assetsDiscoveryRepository = assetsDiscoveryRepository,
        )
    }

    @Provides
    @Singleton
    fun provideAcknowledgeAssetsDiscoveryCompletionUseCase(
        assetsDiscoveryRepository: AssetsDiscoveryRepository,
    ): AcknowledgeAssetsDiscoveryCompletionUseCase {
        return AcknowledgeAssetsDiscoveryCompletionUseCase(
            assetsDiscoveryRepository = assetsDiscoveryRepository,
        )
    }

    @Provides
    @Singleton
    fun provideStartAssetsDiscoveryUseCase(
        assetsDiscoveryRepository: AssetsDiscoveryRepository,
        manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
        analyticsEventHandler: AnalyticsEventHandler,
        appCoroutineScope: AppCoroutineScope,
    ): StartAssetsDiscoveryUseCase {
        return StartAssetsDiscoveryUseCase(
            assetsDiscoveryRepository = assetsDiscoveryRepository,
            manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
            analyticsEventHandler = analyticsEventHandler,
            appCoroutineScope = appCoroutineScope,
        )
    }
}