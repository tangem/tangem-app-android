package com.tangem.datasource.di

import com.tangem.datasource.api.common.config.TangemTech
import com.tangem.datasource.api.common.config.News
import com.tangem.datasource.api.common.config.YieldSupply
import com.tangem.datasource.api.common.config.PolymarketWeb
import com.tangem.datasource.api.common.config.PolymarketRelayer
import com.tangem.datasource.api.common.config.PolymarketClob

import com.tangem.datasource.BuildConfig
import com.tangem.core.remote.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.core.remote.config.ApiConfigs
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.config.managers.DevApiConfigsManager
import com.tangem.datasource.api.common.config.managers.MockApiConfigsManager
import com.tangem.datasource.api.common.config.managers.ProdApiConfigsManager
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.news.NewsApi
import com.tangem.datasource.api.jointaccount.JointAccountApi
import com.tangem.datasource.api.polymarket.PolymarketApi
import com.tangem.datasource.api.polymarket.clob.PolymarketClobApi
import com.tangem.datasource.api.polymarket.geo.PolymarketGeoApi
import com.tangem.datasource.api.polymarket.relayer.PolymarketRelayerApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.YieldSupplyApi
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.build
import com.tangem.core.remote.Timeouts
import com.tangem.datasource.di.utils.RetrofitApiBuilder
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("TooManyFunctions", "LargeClass")
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    private const val TIMEOUT_60_SECONDS = 60L

    @Provides
    @Singleton
    fun provideApiConfigManager(
        apiConfigs: ApiConfigs,
        appPreferencesStore: AppPreferencesStore,
        appScope: AppCoroutineScope,
    ): ApiConfigsManager {
        // The DI key (@StringKey in ApiConfigsModule) and the config's own id are declared separately;
        // fail fast on startup if they drift, otherwise the config would be silently unreachable by id.
        apiConfigs.forEach { (key, config) ->
            check(key == config.id.name) {
                "ApiConfig DI key [$key] doesn't match the config id [${config.id.name}]. " +
                    "The @StringKey in ApiConfigsModule must match the config's own id."
            }
        }

        return when {
            BuildConfig.BUILD_TYPE == MOCKED_BUILD_TYPE -> MockApiConfigsManager(apiConfigs, appScope)
            BuildConfig.TESTER_MENU_ENABLED -> DevApiConfigsManager(apiConfigs, appPreferencesStore, appScope)
            else -> ProdApiConfigsManager(apiConfigs)
        }
    }

    @Provides
    @Singleton
    fun provideTangemTechApi(retrofitApiBuilder: RetrofitApiBuilder): TangemTechApi {
        return retrofitApiBuilder.build(
            RetrofitApiSpec(
                apiConfigId = TangemTech.ID,
                shouldApplyTimeoutAnnotations = true,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyApi(retrofitApiBuilder: RetrofitApiBuilder): YieldSupplyApi {
        return retrofitApiBuilder.build(
            RetrofitApiSpec(
                apiConfigId = YieldSupply.ID,
                shouldApplyTimeoutAnnotations = true,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideJointAccountApi(retrofitApiBuilder: RetrofitApiBuilder): JointAccountApi {
        return retrofitApiBuilder.build(
            RetrofitApiSpec(
                apiConfigId = TangemTech.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideTangemTechMarketsApi(retrofitApiBuilder: RetrofitApiBuilder): TangemTechMarketsApi {
        return retrofitApiBuilder.build(
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
    @Singleton
    fun provideNewsApi(retrofitApiBuilder: RetrofitApiBuilder): NewsApi {
        return retrofitApiBuilder.build(
            RetrofitApiSpec(
                apiConfigId = News.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketApi(retrofitApiBuilder: RetrofitApiBuilder): PolymarketApi {
        // Polymarket BFF Discovery lives on the main Tangem gateway — reuse the TangemTech config.
        return retrofitApiBuilder.build(
            RetrofitApiSpec(
                apiConfigId = TangemTech.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketGeoApi(retrofitApiBuilder: RetrofitApiBuilder): PolymarketGeoApi {
        return retrofitApiBuilder.build(
            RetrofitApiSpec(
                apiConfigId = PolymarketWeb.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketRelayerApi(retrofitApiBuilder: RetrofitApiBuilder): PolymarketRelayerApi {
        return retrofitApiBuilder.build(
            RetrofitApiSpec(
                apiConfigId = PolymarketRelayer.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePolymarketClobApi(retrofitApiBuilder: RetrofitApiBuilder): PolymarketClobApi {
        return retrofitApiBuilder.build(
            RetrofitApiSpec(
                apiConfigId = PolymarketClob.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                shouldSaveLogs = false,
            ),
        )
    }
}