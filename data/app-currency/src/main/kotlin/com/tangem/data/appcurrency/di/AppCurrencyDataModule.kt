package com.tangem.data.appcurrency.di

import com.tangem.data.appcurrency.DefaultAppCurrencyRepository
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.appcurrency.AvailableAppCurrenciesStore
import com.tangem.datasource.local.appcurrency.SelectedAppCurrencyStore
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppCurrencyDataModule {

    @Provides
    @Singleton
    fun provideAppCurrencyRepository(
        tangemTechApi: TangemTechApi,
        availableAppCurrenciesStore: AvailableAppCurrenciesStore,
        selectedAppCurrencyStore: SelectedAppCurrencyStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
    ): AppCurrencyRepository {
        return DefaultAppCurrencyRepository(
            tangemTechApi,
            availableAppCurrenciesStore,
            selectedAppCurrencyStore,
            cacheRegistry,
            dispatchers,
        )
    }
}
