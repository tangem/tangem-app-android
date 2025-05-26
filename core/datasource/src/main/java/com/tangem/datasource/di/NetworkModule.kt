package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.blockaid.BlockAidApi
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfigs
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.config.managers.DevApiConfigsManager
import com.tangem.datasource.api.common.config.managers.ProdApiConfigsManager
import com.tangem.datasource.api.common.response.ApiResponseCallAdapterFactory
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechApiV2
import com.tangem.datasource.api.visa.TangemVisaApi
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.utils.*
import com.tangem.datasource.utils.RequestHeader.AppVersionPlatformHeaders
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    private const val PROD_V2_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v2/"
    private const val TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS = 60L
    private const val STAKE_KIT_API_TIMEOUT_SECONDS = 60L

    private val excludedApiForLogging: Set<ApiConfig.ID> = setOf(
        // ApiConfig.ID.StakeKit,
    )

    @Provides
    @Singleton
    fun provideApiConfigManager(
        apiConfigs: ApiConfigs,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): ApiConfigsManager {
        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DevApiConfigsManager(apiConfigs, appPreferencesStore, dispatchers)
        } else {
            ProdApiConfigsManager(apiConfigs)
        }
    }

    @Provides
    @Singleton
    fun provideExpressApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        analyticsErrorHandler: AnalyticsErrorHandler,
        apiConfigsManager: ApiConfigsManager,
        appLogsStore: AppLogsStore,
    ): TangemExpressApi {
        return createApi(
            id = ApiConfig.ID.Express,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
            analyticsErrorHandler = analyticsErrorHandler,
            clientBuilder = {
                addInterceptor(
                    NetworkLogsSaveInterceptor(appLogsStore),
                )
            },
        )
    }

    @Provides
    @Singleton
    fun provideStakeKitApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        apiConfigsManager: ApiConfigsManager,
        analyticsErrorHandler: AnalyticsErrorHandler,
        appLogsStore: AppLogsStore,
    ): StakeKitApi {
        return createApi(
            id = ApiConfig.ID.StakeKit,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
            analyticsErrorHandler = analyticsErrorHandler,
            timeouts = Timeouts(
                callTimeoutSeconds = STAKE_KIT_API_TIMEOUT_SECONDS,
                connectTimeoutSeconds = STAKE_KIT_API_TIMEOUT_SECONDS,
                readTimeoutSeconds = STAKE_KIT_API_TIMEOUT_SECONDS,
                writeTimeoutSeconds = STAKE_KIT_API_TIMEOUT_SECONDS,
            ),
            clientBuilder = {
                addInterceptor(
                    NetworkLogsSaveInterceptor(appLogsStore),
                )
            },
        )
    }

    @Provides
    @Singleton
    fun provideOnrampApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        analyticsErrorHandler: AnalyticsErrorHandler,
        apiConfigsManager: ApiConfigsManager,
        appLogsStore: AppLogsStore,
    ): OnrampApi {
        return createApi(
            id = ApiConfig.ID.Express,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
            analyticsErrorHandler = analyticsErrorHandler,
            clientBuilder = {
                addInterceptor(
                    NetworkLogsSaveInterceptor(appLogsStore),
                )
            },
        )
    }

    @Provides
    @Singleton
    fun provideTangemTechApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        analyticsErrorHandler: AnalyticsErrorHandler,
        apiConfigsManager: ApiConfigsManager,
    ): TangemTechApi {
        return createApi(
            id = ApiConfig.ID.TangemTech,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
            analyticsErrorHandler = analyticsErrorHandler,
            clientBuilder = { applyTimeoutAnnotations() },
        )
    }

    // TODO: It will be deleted in the future or refactored using ApiConfig
    @Provides
    @Singleton
    fun provideTangemTechApiV2(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        analyticsErrorHandler: AnalyticsErrorHandler,
        appVersionProvider: AppVersionProvider,
        appInfoProvider: AppInfoProvider,
    ): TangemTechApiV2 {
        return provideTangemTechApiInternal(
            moshi = moshi,
            context = context,
            appVersionProvider = appVersionProvider,
            baseUrl = PROD_V2_TANGEM_TECH_BASE_URL,
            analyticsErrorHandler = analyticsErrorHandler,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @Singleton
    fun provideTangemTechMarketsApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        analyticsErrorHandler: AnalyticsErrorHandler,
        apiConfigsManager: ApiConfigsManager,
    ): TangemTechMarketsApi {
        return createApi(
            id = ApiConfig.ID.TangemTech,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
            analyticsErrorHandler = analyticsErrorHandler,
            clientBuilder = {
                this.callTimeout(TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .connectTimeout(TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .applyTimeoutAnnotations()
            },
        )
    }

    @Provides
    @Singleton
    fun provideTangemVisaApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        analyticsErrorHandler: AnalyticsErrorHandler,
        apiConfigsManager: ApiConfigsManager,
        appLogsStore: AppLogsStore,
    ): TangemVisaApi {
        return createApi<TangemVisaApi>(
            id = ApiConfig.ID.TangemVisa,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
            analyticsErrorHandler = analyticsErrorHandler,
            clientBuilder = {
                addInterceptor(
                    NetworkLogsSaveInterceptor(appLogsStore),
                ).applyTimeoutAnnotations()
            },
        )
    }

    @Suppress("LongParameterList")
    @Deprecated("use createApi instead")
    private inline fun <reified T> provideTangemTechApiInternal(
        moshi: Moshi,
        context: Context,
        appVersionProvider: AppVersionProvider,
        appInfoProvider: AppInfoProvider,
        baseUrl: String,
        analyticsErrorHandler: AnalyticsErrorHandler,
        timeouts: Timeouts = Timeouts(),
        requestHeaders: List<RequestHeader> = listOf(AppVersionPlatformHeaders(appVersionProvider, appInfoProvider)),
    ): T {
        val client = OkHttpClient.Builder()
            .applyTimeoutAnnotations()
            .let { builder ->
                var b = builder
                if (timeouts.callTimeoutSeconds != null) {
                    b = b.callTimeout(timeouts.callTimeoutSeconds, TimeUnit.SECONDS)
                }
                if (timeouts.connectTimeoutSeconds != null) {
                    b = b.connectTimeout(timeouts.connectTimeoutSeconds, TimeUnit.SECONDS)
                }
                if (timeouts.readTimeoutSeconds != null) {
                    b = b.readTimeout(timeouts.readTimeoutSeconds, TimeUnit.SECONDS)
                }
                if (timeouts.writeTimeoutSeconds != null) {
                    b = b.writeTimeout(timeouts.writeTimeoutSeconds, TimeUnit.SECONDS)
                }
                b
            }
            .addHeaders(
                *requestHeaders.toTypedArray(),
                // TODO("refactor header init") get auth data after biometric auth to avoid race condition
                // AuthenticationHeader(authProvider),
            )
            .addLoggers(context)
            .build()

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create(analyticsErrorHandler))
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(T::class.java)
    }

    @Provides
    @Singleton
    fun provideBlockAidApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        analyticsErrorHandler: AnalyticsErrorHandler,
        apiConfigsManager: ApiConfigsManager,
        appLogsStore: AppLogsStore,
    ): BlockAidApi {
        return createApi<BlockAidApi>(
            id = ApiConfig.ID.BlockAid,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
            analyticsErrorHandler = analyticsErrorHandler,
            clientBuilder = {
                addInterceptor(
                    NetworkLogsSaveInterceptor(appLogsStore),
                ).applyTimeoutAnnotations()
            },
        )
    }

    private inline fun <reified T> createApi(
        id: ApiConfig.ID,
        moshi: Moshi,
        context: Context,
        apiConfigsManager: ApiConfigsManager,
        analyticsErrorHandler: AnalyticsErrorHandler,
        timeouts: Timeouts = Timeouts(),
        clientBuilder: OkHttpClient.Builder.() -> OkHttpClient.Builder = { this },
    ): T {
        val environmentConfig = apiConfigsManager.getEnvironmentConfig(id)

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create(analyticsErrorHandler))
            .baseUrl(environmentConfig.baseUrl)
            .client(
                OkHttpClient.Builder()
                    .applyApiConfig(id, apiConfigsManager)
                    .applyTimeoutAnnotations()
                    .let { builder ->
                        var b = builder
                        if (timeouts.callTimeoutSeconds != null) {
                            b = b.callTimeout(timeouts.callTimeoutSeconds, TimeUnit.SECONDS)
                        }
                        if (timeouts.connectTimeoutSeconds != null) {
                            b = b.connectTimeout(timeouts.connectTimeoutSeconds, TimeUnit.SECONDS)
                        }
                        if (timeouts.readTimeoutSeconds != null) {
                            b = b.readTimeout(timeouts.readTimeoutSeconds, TimeUnit.SECONDS)
                        }
                        if (timeouts.writeTimeoutSeconds != null) {
                            b = b.writeTimeout(timeouts.writeTimeoutSeconds, TimeUnit.SECONDS)
                        }
                        b
                    }
                    .addLoggers(context = context, id = id)
                    .clientBuilder()
                    .build(),
            )
            .build()
            .create(T::class.java)
    }

    private fun OkHttpClient.Builder.addLoggers(context: Context, id: ApiConfig.ID): OkHttpClient.Builder {
        if (id in excludedApiForLogging) return this

        return addLoggers(context)
    }

    private data class Timeouts(
        val callTimeoutSeconds: Long? = null,
        val connectTimeoutSeconds: Long? = null,
        val readTimeoutSeconds: Long? = null,
        val writeTimeoutSeconds: Long? = null,
    )
}