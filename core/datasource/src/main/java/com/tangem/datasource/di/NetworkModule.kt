package com.tangem.datasource.di

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.blockaid.BlockAidApi
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfigs
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.config.managers.DevApiConfigsManager
import com.tangem.datasource.api.common.config.managers.MockApiConfigsManager
import com.tangem.datasource.api.common.config.managers.ProdApiConfigsManager
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.moonpay.MoonPayApi
import com.tangem.datasource.api.news.NewsApi
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.gasless.GaslessTxServiceApi
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.TangemPayAuthApi
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.YieldSupplyApi
import com.tangem.datasource.api.visa.VisaApi
import com.tangem.datasource.di.utils.RetrofitApiBuilder
import com.tangem.datasource.di.utils.RetrofitApiBuilder.Timeouts
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    private const val TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS = 60L
    private const val STAKE_KIT_API_TIMEOUT_SECONDS = 60L

    private const val P2P_ETH_POOL_API_TIMEOUT_SECONDS = 60L

    @Provides
    @Singleton
    fun provideApiConfigManager(
        apiConfigs: ApiConfigs,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): ApiConfigsManager {
        return when {
            BuildConfig.BUILD_TYPE == MOCKED_BUILD_TYPE -> MockApiConfigsManager(apiConfigs, dispatchers)
            BuildConfig.TESTER_MENU_ENABLED -> DevApiConfigsManager(apiConfigs, appPreferencesStore, dispatchers)
            else -> ProdApiConfigsManager(apiConfigs)
        }
    }

    @Provides
    @Singleton
    fun provideExpressApi(retrofitApiBuilder: RetrofitApiBuilder): TangemExpressApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.Express,
            applyTimeoutAnnotations = false,
        )
    }

    @Provides
    @Singleton
    fun provideStakeKitApi(retrofitApiBuilder: RetrofitApiBuilder): StakeKitApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.StakeKit,
            applyTimeoutAnnotations = false,
            timeouts = Timeouts(
                callTimeoutSeconds = STAKE_KIT_API_TIMEOUT_SECONDS,
                connectTimeoutSeconds = STAKE_KIT_API_TIMEOUT_SECONDS,
                readTimeoutSeconds = STAKE_KIT_API_TIMEOUT_SECONDS,
                writeTimeoutSeconds = STAKE_KIT_API_TIMEOUT_SECONDS,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideP2PEthPoolApi(retrofitApiBuilder: RetrofitApiBuilder): P2PEthPoolApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.P2PEthPool,
            applyTimeoutAnnotations = false,
            timeouts = Timeouts(
                callTimeoutSeconds = P2P_ETH_POOL_API_TIMEOUT_SECONDS,
                connectTimeoutSeconds = P2P_ETH_POOL_API_TIMEOUT_SECONDS,
                readTimeoutSeconds = P2P_ETH_POOL_API_TIMEOUT_SECONDS,
                writeTimeoutSeconds = P2P_ETH_POOL_API_TIMEOUT_SECONDS,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideOnrampApi(retrofitApiBuilder: RetrofitApiBuilder): OnrampApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.Express,
            applyTimeoutAnnotations = false,
        )
    }

    @Provides
    @Singleton
    fun provideTangemTechApi(retrofitApiBuilder: RetrofitApiBuilder): TangemTechApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.TangemTech,
            applyTimeoutAnnotations = true,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyApi(retrofitApiBuilder: RetrofitApiBuilder): YieldSupplyApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.YieldSupply,
            applyTimeoutAnnotations = true,
        )
    }

    @Provides
    @Singleton
    fun provideTangemTechMarketsApi(retrofitApiBuilder: RetrofitApiBuilder): TangemTechMarketsApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.TangemTech,
            applyTimeoutAnnotations = false,
            timeouts = Timeouts(
                callTimeoutSeconds = TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS,
                connectTimeoutSeconds = TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS,
                readTimeoutSeconds = TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS,
            ),
            logsSaving = false,
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayApi(retrofitApiBuilder: RetrofitApiBuilder): TangemPayApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.TangemPay,
            applyTimeoutAnnotations = false,
        )
    }

    @Provides
    @Singleton
    fun provideVisaApi(retrofitApiBuilder: RetrofitApiBuilder): VisaApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.TangemPay,
            applyTimeoutAnnotations = false,
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayAuthApi(retrofitApiBuilder: RetrofitApiBuilder): TangemPayAuthApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.TangemPayAuth,
            applyTimeoutAnnotations = false,
        )
    }

    @Provides
    @Singleton
    fun provideBlockAidApi(retrofitApiBuilder: RetrofitApiBuilder): BlockAidApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.BlockAid,
            applyTimeoutAnnotations = false,
        )
    }

    @Provides
    @Singleton
    fun provideMoonPayApi(retrofitApiBuilder: RetrofitApiBuilder): MoonPayApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.MoonPay,
            applyTimeoutAnnotations = false,
        )
    }

    @Provides
    @Singleton
    fun provideNewsApi(retrofitApiBuilder: RetrofitApiBuilder): NewsApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.News,
            applyTimeoutAnnotations = false,
        )
    }

    @Provides
    @Singleton
    fun provideGaslessTxServiceApi(retrofitApiBuilder: RetrofitApiBuilder): GaslessTxServiceApi {
        return retrofitApiBuilder.build(
            apiConfigId = ApiConfig.ID.GaslessTxService,
            applyTimeoutAnnotations = false,
        )
    }
}