package com.tangem.data.visa.di

import com.squareup.moshi.Moshi
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.visa.BuildConfig
import com.tangem.data.visa.DefaultVisaRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.lib.visa.VisaContractInfoProvider
import com.tangem.lib.visa.api.VisaApiBuilder
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
        @NetworkMoshi moshi: Moshi,
        tangemTechApi: TangemTechApi,
        cacheRegistry: CacheRegistry,
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): VisaRepository {
        val contractInfoProvider = VisaContractInfoProvider.Builder(
            isNetworkLoggingEnabled = BuildConfig.LOG_ENABLED,
            dispatchers = dispatchers,
        ).build()
        val visaApi = VisaApiBuilder(
            useDevApi = true,
            isNetworkLoggingEnabled = BuildConfig.LOG_ENABLED,
            moshi = moshi,
        ).build()

        return DefaultVisaRepository(
            contractInfoProvider,
            tangemTechApi,
            visaApi,
            cacheRegistry,
            userWalletsStore,
            dispatchers,
        )
    }
}
