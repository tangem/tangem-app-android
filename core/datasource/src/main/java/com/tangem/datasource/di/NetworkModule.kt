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
internal object NetworkModule {

    private const val DEV_V1_TANGEM_TECH_BASE_URL = "https://devapi.tangem-tech.com/v1/"
    private const val PROD_V2_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v2/"
    private const val TANGEM_TECH_MARKETS_SERVICE_TIMEOUT_SECONDS = 60L

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
        return createApi(
            id = ApiConfig.ID.Express,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
        )
    }

    @Provides
    @Singleton
    fun provideStakeKitApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        apiConfigsManager: ApiConfigsManager,
    ): StakeKitApi {
        return createApi(
            id = ApiConfig.ID.StakeKit,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
        )
    }

    @Provides
    @Singleton
    fun provideTangemTechApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        apiConfigsManager: ApiConfigsManager,
    ): TangemTechApi {
        return createApi(
            id = ApiConfig.ID.TangemTech,
            moshi = moshi,
            context = context,
            apiConfigsManager = apiConfigsManager,
        )
    }

    // TODO: It will be deleted in the future or refactored using ApiConfig
    @Provides
    @Singleton
    fun provideTangemTechApiV2(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
    ): TangemTechApiV2 {
        return provideTangemTechApiInternal(
            moshi = moshi,
            context = context,
            appVersionProvider = appVersionProvider,
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
    ): TangemTechApi {
        return provideTangemTechApiInternal(
            moshi = moshi,
            context = context,
            appVersionProvider = appVersionProvider,
            baseUrl = DEV_V1_TANGEM_TECH_BASE_URL,
        )
    }

    // TODO: [REDACTED_JIRA]
    @Provides
    @DevTangemApi
    @Singleton
    fun provideTangemTechMarketsApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
    ): TangemTechMarketsApi {
        return provideTangemTechApiInternal(
            moshi = moshi,
            context = context,
            appVersionProvider = appVersionProvider,
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
        baseUrl: String,
        timeouts: Timeouts = Timeouts(),
        requestHeaders: List<RequestHeader> = listOf(AppVersionPlatformHeaders(appVersionProvider)),
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
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(T::class.java)
    }

    private inline fun <reified T> createApi(
        id: ApiConfig.ID,
        moshi: Moshi,
        context: Context,
        apiConfigsManager: ApiConfigsManager,
    ): T {
        val environmentConfig = apiConfigsManager.getEnvironmentConfig(id)

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(environmentConfig.baseUrl)
            .client(
                OkHttpClient.Builder()
                    .applyApiConfig(id, apiConfigsManager)
                    .applyTimeoutAnnotations()
                    .addLoggers(context)
                    .build(),
            )
            .build()
            .create(T::class.java)
    }

    private data class Timeouts(
        val callTimeoutSeconds: Long? = null,
        val connectTimeoutSeconds: Long? = null,
        val readTimeoutSeconds: Long? = null,
        val writeTimeoutSeconds: Long? = null,
    )
}