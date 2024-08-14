package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfigs
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.config.managers.DevApiConfigsManager
import com.tangem.datasource.api.common.config.managers.ProdApiConfigsManager
import com.tangem.datasource.api.common.response.ApiResponseCallAdapterFactory
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechApiV2
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.utils.*
import com.tangem.datasource.utils.RequestHeader.AppVersionPlatformHeaders
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
class NetworkModule {

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
        apiConfigsManager: ApiConfigsManager,
    ): TangemExpressApi {
        val environmentConfig = apiConfigsManager.getEnvironmentConfig(id = ApiConfig.ID.Express)

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(environmentConfig.baseUrl)
            .client(
                OkHttpClient.Builder()
                    .applyApiConfig(ApiConfig.ID.Express, apiConfigsManager)
                    .addLoggers(context)
                    .build(),
            )
            .build()
            .create(TangemExpressApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStakeKitApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        apiConfigsManager: ApiConfigsManager,
    ): StakeKitApi {
        val environmentConfig = apiConfigsManager.getEnvironmentConfig(id = ApiConfig.ID.StakeKit)

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(environmentConfig.baseUrl)
            .client(
                OkHttpClient.Builder()
                    .applyApiConfig(id = ApiConfig.ID.StakeKit, apiConfigsManager = apiConfigsManager)
                    .addLoggers(context)
                    .build(),
            )
            .build()
            .create(StakeKitApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTangemTechApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        apiConfigsManager: ApiConfigsManager,
    ): TangemTechApi {
        val environmentConfig = apiConfigsManager.getEnvironmentConfig(id = ApiConfig.ID.TangemTech)

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(environmentConfig.baseUrl)
            .client(
                OkHttpClient.Builder()
                    .applyApiConfig(id = ApiConfig.ID.TangemTech, apiConfigsManager = apiConfigsManager)
                    .applyTimeoutAnnotations()
                    .addLoggers(context)
                    .build(),
            )
            .build()
            .create(TangemTechApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTangemTechApiV2(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
        apiConfigsManager: ApiConfigsManager,
    ): TangemTechApiV2 {
        return provideTangemTechApiInternal(
            moshi = moshi,
            context = context,
            appVersionProvider = appVersionProvider,
            apiConfigsManager = apiConfigsManager,
            baseUrl = PROD_V2_TANGEM_TECH_BASE_URL,
        )
    }

    @Provides
    @DevTangemApi
    @Singleton
    fun provideTangemTechDevApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
        apiConfigsManager: ApiConfigsManager,
    ): TangemTechApi {
        return provideTangemTechApiInternal(
            moshi = moshi,
            context = context,
            appVersionProvider = appVersionProvider,
            apiConfigsManager = apiConfigsManager,
            baseUrl = DEV_V1_TANGEM_TECH_BASE_URL,
        )
    }

    @Provides
    @DevTangemApi
    @Singleton
    fun provideTangemTechMarketsApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
        apiConfigsManager: ApiConfigsManager,
    ): TangemTechMarketsApi {
        return provideTangemTechApiInternal(
            moshi = moshi,
            context = context,
            appVersionProvider = appVersionProvider,
            apiConfigsManager = apiConfigsManager,
            baseUrl = DEV_V1_TANGEM_TECH_BASE_URL,
            timeouts = Timeouts(
                callTimeoutSeconds = TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS,
                connectTimeoutSeconds = TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS,
                readTimeoutSeconds = TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS,
            ),
            requestHeaders = listOf(AppVersionPlatformHeaders(appVersionProvider)),
        )
    }

    private inline fun <reified T> provideTangemTechApiInternal(
        moshi: Moshi,
        context: Context,
        appVersionProvider: AppVersionProvider,
        apiConfigsManager: ApiConfigsManager,
        baseUrl: String,
        timeouts: Timeouts = Timeouts(),
        requestHeaders: List<RequestHeader> = listOf(AppVersionPlatformHeaders(appVersionProvider)),
    ): T {
        val client = OkHttpClient.Builder()
            .applyApiConfig(id = ApiConfig.ID.TangemTech, apiConfigsManager = apiConfigsManager)
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
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(T::class.java)
    }

    private data class Timeouts(
        val callTimeoutSeconds: Long? = null,
        val connectTimeoutSeconds: Long? = null,
        val readTimeoutSeconds: Long? = null,
        val writeTimeoutSeconds: Long? = null,
    )

    private companion object {
        const val DEV_V1_TANGEM_TECH_BASE_URL = "https://devapi.tangem-tech.com/v1/"

        const val PROD_V2_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v2/"

        const val TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS = 60L
    }
}