package com.tangem.grow.datasource.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.Timeouts
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.header.CardAuthHeaderProvider
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
import com.tangem.grow.datasource.config.Express
import com.tangem.grow.datasource.config.GaslessTxService
import com.tangem.grow.datasource.config.GrowEnvironmentConfig
import com.tangem.grow.datasource.config.MoonPay
import com.tangem.grow.datasource.config.P2PEthPool
import com.tangem.grow.datasource.config.StakeKit
import com.tangem.grow.datasource.config.YieldSupply
import com.tangem.grow.datasource.ethpool.P2PEthPoolApi
import com.tangem.grow.datasource.yield.YieldSupplyApi
import com.tangem.grow.datasource.express.ExpressAuthProvider
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.grow.datasource.gasless.GaslessTxServiceApi
import com.tangem.grow.datasource.gasless.GaslessTxServiceApiV2
import com.tangem.grow.datasource.gasless.TronGaslessApi
import com.tangem.grow.datasource.moonpay.MoonPayApi
import com.tangem.grow.datasource.onramp.OnrampApi
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.addStakeKitEnumFallbackAdapters
import com.tangem.grow.datasource.yield.local.DefaultYieldMarketsStore
import com.tangem.grow.datasource.yield.local.YieldMarketsStore
import com.tangem.grow.datasource.yield.models.YieldSupplyMarketTokenDto
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.info.AppInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object GrowDatasourceModule {

    private const val TIMEOUT_60_SECONDS = 60L
    private const val TIMEOUT_90_SECONDS = 90L

    @Provides
    @IntoMap
    @StringKey(MoonPay.KEY)
    fun provideMoonPayConfig(): ApiConfig {
        return MoonPay()
    }

    @Provides
    @Singleton
    fun provideMoonPayApi(factory: RetrofitFactory): MoonPayApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = MoonPay.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @IntoMap
    @StringKey(GaslessTxService.KEY)
    fun provideGaslessConfig(
        growEnvironmentConfig: GrowEnvironmentConfig,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return GaslessTxService(
            growEnvironmentConfig = growEnvironmentConfig,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @Singleton
    fun provideGaslessTxServiceApi(factory: RetrofitFactory): GaslessTxServiceApi {
        return factory.build(gaslessApiSpec())
    }

    @Provides
    @Singleton
    fun provideGaslessTxServiceApiV2(factory: RetrofitFactory): GaslessTxServiceApiV2 {
        return factory.build(gaslessApiSpec())
    }

    @Provides
    @Singleton
    fun provideTronGaslessApi(factory: RetrofitFactory): TronGaslessApi {
        return factory.build(gaslessApiSpec())
    }

    private fun gaslessApiSpec() = RetrofitApiSpec(
        apiConfigId = GaslessTxService.ID,
        shouldApplyTimeoutAnnotations = false,
        shouldUseSessionAuth = false,
        timeouts = Timeouts(
            callTimeoutSeconds = TIMEOUT_60_SECONDS,
            connectTimeoutSeconds = TIMEOUT_60_SECONDS,
            readTimeoutSeconds = TIMEOUT_60_SECONDS,
            writeTimeoutSeconds = TIMEOUT_60_SECONDS,
        ),
    )

    @Provides
    @IntoMap
    @StringKey(Express.KEY)
    fun provideExpressConfig(
        growEnvironmentConfig: GrowEnvironmentConfig,
        expressAuthProvider: ExpressAuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return Express(
            growEnvironmentConfig = growEnvironmentConfig,
            expressAuthProvider = expressAuthProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @Singleton
    fun provideExpressApi(factory: RetrofitFactory): TangemExpressApi {
        return factory.build(expressApiSpec())
    }

    @Provides
    @Singleton
    fun provideOnrampApi(factory: RetrofitFactory): OnrampApi {
        return factory.build(expressApiSpec())
    }

    private fun expressApiSpec() = RetrofitApiSpec(
        apiConfigId = Express.ID,
        shouldApplyTimeoutAnnotations = false,
        shouldUseSessionAuth = false,
    )

    @Provides
    @IntoMap
    @StringKey(StakeKit.KEY)
    fun provideStakeKitConfig(growEnvironmentConfig: GrowEnvironmentConfig): ApiConfig {
        return StakeKit(growEnvironmentConfig)
    }

    @Provides
    @Singleton
    fun provideStakeKitApi(factory: RetrofitFactory): StakeKitApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = StakeKit.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                timeouts = Timeouts(
                    callTimeoutSeconds = TIMEOUT_60_SECONDS,
                    connectTimeoutSeconds = TIMEOUT_60_SECONDS,
                    readTimeoutSeconds = TIMEOUT_60_SECONDS,
                    writeTimeoutSeconds = TIMEOUT_60_SECONDS,
                ),
            ),
        )
    }

    @Provides
    @IntoSet
    fun provideStakeKitEnumFallbackConfigurer(): NetworkMoshiConfigurer {
        return NetworkMoshiConfigurer { it.addStakeKitEnumFallbackAdapters() }
    }

    @Provides
    @IntoMap
    @StringKey(YieldSupply.KEY)
    fun provideYieldSupplyConfig(
        growEnvironmentConfig: GrowEnvironmentConfig,
        cardAuthHeaderProvider: CardAuthHeaderProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return YieldSupply(
            growEnvironmentConfig = growEnvironmentConfig,
            cardAuthHeader = cardAuthHeaderProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyApi(factory: RetrofitFactory): YieldSupplyApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = YieldSupply.ID,
                shouldApplyTimeoutAnnotations = true,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideYieldMarketsStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
        dataStoreFactory: AppDataStoreFactory,
    ): YieldMarketsStore {
        return DefaultYieldMarketsStore(
            persistenceStore = dataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = listTypes<YieldSupplyMarketTokenDto>(),
                    defaultValue = emptyList(),
                ),
                produceFile = { context.dataStoreFile(fileName = "yield_markets_cache") },
                scope = appScope,
            ),
        )
    }

    @Provides
    @IntoMap
    @StringKey(P2PEthPool.KEY)
    fun provideP2PEthPoolConfig(growEnvironmentConfig: GrowEnvironmentConfig): ApiConfig {
        return P2PEthPool(growEnvironmentConfig)
    }

    @Provides
    @Singleton
    fun provideP2PEthPoolApi(factory: RetrofitFactory): P2PEthPoolApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = P2PEthPool.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                timeouts = Timeouts(
                    callTimeoutSeconds = TIMEOUT_90_SECONDS,
                    connectTimeoutSeconds = TIMEOUT_90_SECONDS,
                    readTimeoutSeconds = TIMEOUT_90_SECONDS,
                    writeTimeoutSeconds = TIMEOUT_90_SECONDS,
                ),
            ),
        )
    }
}