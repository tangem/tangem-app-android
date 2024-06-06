package com.tangem.data.visa.di

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.visa.DefaultVisaRepository
import com.tangem.data.visa.config.VisaLibLoader
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ImplementedVisaDataModule {

    @Provides
    @Singleton
    @ImplementedVisaRepository
    fun provideVisaRepository(
        visaLibLoader: VisaLibLoader,
        tangemTechApi: TangemTechApi,
        cacheRegistry: CacheRegistry,
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): VisaRepository = DefaultVisaRepository(
        visaLibLoader,
        tangemTechApi,
        cacheRegistry,
        userWalletsStore,
        dispatchers,
    )
}