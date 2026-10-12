package com.tangem.grow.datasource.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.Timeouts
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.core.remote.header.CardAuthHeaderProvider
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
import com.tangem.grow.datasource.collectibles.CollectiblesApi
import com.tangem.grow.datasource.collectibles.CollectiblesAuthProvider
import com.tangem.grow.datasource.collectibles.models.response.model.DefaultCollectiblesAuthProvider
import com.tangem.grow.datasource.config.*
import com.tangem.grow.datasource.ethpool.P2PEthPoolApi
import com.tangem.grow.datasource.express.DefaultExpressAuthProvider
import com.tangem.grow.datasource.express.ExpressAuthProvider
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.grow.datasource.gasless.GaslessTxServiceApi
import com.tangem.grow.datasource.gasless.GaslessTxServiceApiV2
import com.tangem.grow.datasource.gasless.TronGaslessApi
import com.tangem.grow.datasource.moonpay.MoonPayApi
import com.tangem.grow.datasource.onramp.OnrampApi
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.bridge.PolymarketBridgeApi
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobApi
import com.tangem.grow.datasource.polymarket.data.PolymarketDataApi
import com.tangem.grow.datasource.polymarket.geo.PolymarketGeoApi
import com.tangem.grow.datasource.polymarket.relayer.PolymarketRelayerApi
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.addStakeKitEnumFallbackAdapters
import com.tangem.grow.datasource.yield.YieldIncentivesApi
import com.tangem.grow.datasource.yield.YieldSupplyApi
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

@Suppress("TooManyFunctions", "LargeClass")
@Module
@InstallIn(SingletonComponent::class)
internal object GrowDatasourceModule {

    private const val TIMEOUT_60_SECONDS = 60L
    private const val TIMEOUT_90_SECONDS = 90L
    private const val TIMEOUT_120_SECONDS = 120L

    @Provides
    @Singleton
    fun provideExpressAuthProvider(): ExpressAuthProvider {
        return DefaultExpressAuthProvider()
    }

    @Provides
    @Singleton
    fun provideCollectiblesAuthProvider(): CollectiblesAuthProvider {
        return DefaultCollectiblesAuthProvider()
    }

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
        return factory.build(tronGaslessApiSpec())
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

    private fun tronGaslessApiSpec() = RetrofitApiSpec(
        apiConfigId = GaslessTxService.ID,
        shouldApplyTimeoutAnnotations = true,
        shouldUseSessionAuth = false,
        timeouts = Timeouts(
            callTimeoutSeconds = TIMEOUT_120_SECONDS,
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
    fun provideYieldIncentivesApi(factory: RetrofitFactory): YieldIncentivesApi {
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

    @Provides
    @IntoMap
    @StringKey(PolymarketWeb.KEY)
    fun providePolymarketWebConfig(): ApiConfig {
        return PolymarketWeb()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketData.KEY)
    fun providePolymarketDataConfig(): ApiConfig {
        return PolymarketData()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketRelayer.KEY)
    fun providePolymarketRelayerConfig(): ApiConfig {
        return PolymarketRelayer()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketClob.KEY)
    fun providePolymarketClobConfig(): ApiConfig {
        return PolymarketClob()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketBridge.KEY)
    fun providePolymarketBridgeConfig(): ApiConfig {
        return PolymarketBridge()
    }

    @Provides
    @Singleton
    fun providePolymarketBridgeApi(factory: RetrofitFactory): PolymarketBridgeApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = PolymarketBridge.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketApi(factory: RetrofitFactory): PolymarketApi {
        // Polymarket BFF Discovery lives on the main Tangem gateway — reuse the TangemTech config.
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemGatewayApiConfig.TANGEM_TECH_ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketGeoApi(factory: RetrofitFactory): PolymarketGeoApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = PolymarketWeb.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketDataApi(factory: RetrofitFactory): PolymarketDataApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = PolymarketData.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketRelayerApi(factory: RetrofitFactory): PolymarketRelayerApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = PolymarketRelayer.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketClobApi(factory: RetrofitFactory): PolymarketClobApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = PolymarketClob.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                shouldSaveLogs = false,
            ),
        )
    }

    @Provides
    @IntoMap
    @StringKey(Collectibles.KEY)
    fun provideCollectiblesConfig(
        collectiblesAuthProvider: CollectiblesAuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return Collectibles(
            collectiblesAuthProvider = collectiblesAuthProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @Singleton
    fun provideCollectiblesApi(factory: RetrofitFactory): CollectiblesApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = Collectibles.ID,
                shouldApplyTimeoutAnnotations = true,
                shouldUseSessionAuth = true,
            ),
        )
    }
}