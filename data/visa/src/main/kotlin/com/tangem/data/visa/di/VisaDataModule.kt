package com.tangem.data.visa.di

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.visa.BuildConfig
import com.tangem.data.visa.DefaultVisaRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.lib.visa.VisaContractInfoProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object VisaDataModule {

    @Provides
    @Singleton
    fun provideVisaRepository(
        tangemTechApi: TangemTechApi,
        cacheRegistry: CacheRegistry,
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): VisaRepository {
        val contractInfoProvider = VisaContractInfoProvider.Builder(
            isNetworkLoggingEnabled = BuildConfig.LOG_ENABLED,
            dispatchers = dispatchers,
        ).build()

        return DefaultVisaRepository(
            contractInfoProvider,
            tangemTechApi,
            cacheRegistry,
            userWalletsStore,
            dispatchers,
        )
    }
}