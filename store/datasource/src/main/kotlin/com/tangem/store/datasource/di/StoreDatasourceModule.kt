package com.tangem.store.datasource.di

import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.Timeouts
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.store.datasource.api.TangemTech
import com.tangem.store.datasource.addressbook.AddressBookApi
import com.tangem.store.datasource.blockaid.BlockAidApi
import com.tangem.store.datasource.config.BlockAid
import com.tangem.store.datasource.config.StoreEnvironmentConfig
import com.tangem.store.datasource.config.SurveySparrow
import com.tangem.store.datasource.markets.TangemTechMarketsApi
import com.tangem.store.datasource.markets.addCoinIndicatorsEnumFallbackAdapters
import com.tangem.store.datasource.surveysparrow.SurveySparrowApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StoreDatasourceModule {

    private const val TIMEOUT_60_SECONDS = 60L

    @Provides
    @IntoMap
    @StringKey(SurveySparrow.KEY)
    fun provideSurveySparrowConfig(storeEnvironmentConfig: StoreEnvironmentConfig): ApiConfig {
        return SurveySparrow(storeEnvironmentConfig)
    }

    @Provides
    @Singleton
    fun provideSurveySparrowApi(factory: RetrofitFactory): SurveySparrowApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = SurveySparrow.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @IntoMap
    @StringKey(BlockAid.KEY)
    fun provideBlockAidConfig(storeEnvironmentConfig: StoreEnvironmentConfig): ApiConfig {
        return BlockAid(storeEnvironmentConfig)
    }

    @Provides
    @Singleton
    fun provideBlockAidApi(factory: RetrofitFactory): BlockAidApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = BlockAid.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideAddressBookApi(factory: RetrofitFactory): AddressBookApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemTech.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideTangemTechMarketsApi(factory: RetrofitFactory): TangemTechMarketsApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemTech.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                timeouts = Timeouts(
                    callTimeoutSeconds = TIMEOUT_60_SECONDS,
                    connectTimeoutSeconds = TIMEOUT_60_SECONDS,
                    readTimeoutSeconds = TIMEOUT_60_SECONDS,
                ),
                shouldSaveLogs = false,
            ),
        )
    }

    @Provides
    @IntoSet
    fun provideCoinIndicatorsEnumFallbackConfigurer(): NetworkMoshiConfigurer {
        return NetworkMoshiConfigurer { it.addCoinIndicatorsEnumFallbackAdapters() }
    }
}