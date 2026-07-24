package com.tangem.datasource.di.utils

import com.tangem.datasource.api.common.config.MoonPay

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.Timeouts
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.auth.qualifier.SessionAuthAuthenticator
import com.tangem.datasource.api.auth.qualifier.SessionAuthInterceptor
import com.tangem.datasource.api.common.SwitchEnvironmentInterceptor
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiConfigs
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.api.common.response.ApiResponseCallAdapterFactory
import com.tangem.datasource.api.utils.ConnectTimeout
import com.tangem.datasource.api.utils.ReadTimeout
import com.tangem.datasource.api.utils.WriteTimeout
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.logs.SensitiveUrlMasker
import com.tangem.datasource.utils.NetworkLogsSaveInterceptor
import com.tangem.datasource.utils.WireMockRedirectInterceptor
import com.tangem.datasource.utils.addHeaders
import com.tangem.utils.JsonStringValuesExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Invocation
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * A builder class for creating Retrofit API instances
 *
 * @property apiConfigsManager     manages API configurations for different environments
 * @property moshi                 moshi
 * @property analyticsErrorHandler handles analytics-related errors
 * @property context               application context
 * @property appLogsStore          application logs store
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@Singleton
internal class RetrofitApiBuilder @Inject constructor(
    private val apiConfigs: ApiConfigs,
    private val apiConfigsManager: ApiConfigsManager,
    @NetworkMoshi private val moshi: Moshi,
    private val analyticsErrorHandler: AnalyticsErrorHandler,
    @ApplicationContext private val context: Context,
    private val appLogsStore: AppLogsStore,
    private val environmentConfig: EnvironmentConfig,
    @SessionAuthInterceptor private val sessionAuthInterceptor: Provider<Interceptor>,
    @SessionAuthAuthenticator private val sessionAuthenticator: Provider<Authenticator>,
    @Named("isBackendAuthenticationEnabled") private val isBackendAuthEnabled: Provider<Boolean>,
) : RetrofitFactory {

    private val configsBaseUrls: Map<ApiConfig.ID, Set<String>> = getConfigsBaseUrls()

    private val sensitiveUrlMasker: SensitiveUrlMasker by lazy {
        val json = Json.encodeToJsonElement(EnvironmentConfig.serializer(), environmentConfig)
        // Drop URL-shaped values (e.g. public endpoint URLs from config); they are not secrets
        // and would obscure unrelated requests in logs.
        val values = JsonStringValuesExtractor.extract(json)
            .filter { it.isNotBlank() && !it.startsWith("http", ignoreCase = true) }
        SensitiveUrlMasker(values)
    }

    /**
     * Builds a Retrofit API instance of [clazz] according to [spec] (see [RetrofitApiSpec] for the
     * available options).
     */
    override fun <T : Any> create(clazz: Class<T>, spec: RetrofitApiSpec): T = createApi(
        clazz = clazz,
        apiConfigId = spec.apiConfigId,
        applyTimeoutAnnotations = spec.shouldApplyTimeoutAnnotations,
        sessionAuth = spec.shouldUseSessionAuth,
        timeouts = spec.timeouts,
        logsSaving = spec.shouldSaveLogs,
    )

    private fun <T : Any> createApi(
        clazz: Class<T>,
        apiConfigId: ApiConfig.ID,
        applyTimeoutAnnotations: Boolean,
        sessionAuth: Boolean,
        timeouts: Timeouts?,
        logsSaving: Boolean,
    ): T {
        val environmentConfig = apiConfigsManager.getEnvironmentConfig(apiConfigId)

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create(analyticsErrorHandler))
            .baseUrl(environmentConfig.baseUrl)
            .client(
                OkHttpClient.Builder()
                    .applyApiConfig(apiConfigId = apiConfigId, environmentConfig = environmentConfig)
                    .applyWireMockRedirect()
                    .applySessionAuth(sessionAuth)
                    .let {
                        if (applyTimeoutAnnotations) it.applyTimeoutAnnotations() else it
                    }
                    .applyTimeouts(timeouts = timeouts)
                    .let {
                        if (logsSaving) it.applyLogsSaving() else it
                    }
                    .addLoggers(apiConfigId = apiConfigId, context = context)
                    .build(),
            )
            .build()
            .create(clazz)
    }

    private fun OkHttpClient.Builder.applySessionAuth(condition: Boolean): OkHttpClient.Builder {
        // Belt-and-suspenders: callers opt in via the `sessionAuth` flag, but if the backend-auth
        // feature toggle is OFF we skip installing the hooks entirely (avoids wiring up DPoP
        // header generation and 401 retry logic on builds where auth isn't live yet).
        if (condition && isBackendAuthEnabled.get()) {
            // Resolve the providers lazily, at request time, instead of eagerly here. The session
            // authenticator depends (via SessionTokenRefresher) back on AuthApi, so calling `.get()`
            // while AuthApi is still being built would recurse into provideAuthApi → applySessionAuth
            // → `.get()` → … and overflow the stack. Deferring `.get()` to the first HTTP call lets
            // AuthApi finish constructing (and get cached) first, breaking the cycle.
            addInterceptor(Interceptor { chain -> sessionAuthInterceptor.get().intercept(chain) })
            authenticator { route, response -> sessionAuthenticator.get().authenticate(route, response) }
        }

        return this
    }

    private fun getConfigsBaseUrls(): Map<ApiConfig.ID, Set<String>> {
        return apiConfigs.values.associate { config ->
            val allBaseUrls = config.environmentConfigs.mapTo(hashSetOf(), ApiEnvironmentConfig::baseUrl)

            config.id to allBaseUrls
        }
    }

    private fun OkHttpClient.Builder.applyApiConfig(
        apiConfigId: ApiConfig.ID,
        environmentConfig: ApiEnvironmentConfig,
    ): OkHttpClient.Builder {
        return if (BuildConfig.TESTER_MENU_ENABLED) {
            addInterceptor(
                interceptor = SwitchEnvironmentInterceptor(
                    id = apiConfigId,
                    baseUrls = configsBaseUrls[apiConfigId]
                        ?: error("Base URLs for ApiConfig with id [$apiConfigId] not found"),
                    apiConfigsManager = apiConfigsManager,
                ),
            )
        } else {
            val headers = environmentConfig.headers

            this.addHeaders(headers)
        }
    }

    private fun OkHttpClient.Builder.applyTimeouts(timeouts: Timeouts?): OkHttpClient.Builder {
        if (timeouts == null) return this

        var b = this
        timeouts.callTimeoutSeconds?.let { b = b.callTimeout(it, TimeUnit.SECONDS) }
        timeouts.connectTimeoutSeconds?.let { b = b.connectTimeout(it, TimeUnit.SECONDS) }
        timeouts.readTimeoutSeconds?.let { b = b.readTimeout(it, TimeUnit.SECONDS) }
        timeouts.writeTimeoutSeconds?.let { b = b.writeTimeout(it, TimeUnit.SECONDS) }

        return b
    }

    /**
     * Apply timeout annotations [Interceptor].
     * Add this [Interceptor] to [OkHttpClient] if use timeout annotations for retrofit requests.
     */
    private fun OkHttpClient.Builder.applyTimeoutAnnotations(): OkHttpClient.Builder {
        return addInterceptor(
            Interceptor { chain ->
                val request = chain.request()
                val tag = request.tag(Invocation::class.java)
                val connectionTimeout = tag?.method()?.getAnnotation(ConnectTimeout::class.java)
                val readTimeout = tag?.method()?.getAnnotation(ReadTimeout::class.java)
                val writeTimeout = tag?.method()?.getAnnotation(WriteTimeout::class.java)

                chain
                    .run {
                        connectionTimeout?.let { withConnectTimeout(timeout = it.duration, unit = it.unit) } ?: this
                    }
                    .run {
                        readTimeout?.let { withReadTimeout(timeout = it.duration, unit = it.unit) } ?: this
                    }
                    .run {
                        writeTimeout?.let { withWriteTimeout(timeout = it.duration, unit = it.unit) } ?: this
                    }
                    .proceed(request)
            },
        )
    }

    private fun OkHttpClient.Builder.applyLogsSaving(): OkHttpClient.Builder {
        return addInterceptor(
            interceptor = NetworkLogsSaveInterceptor(appLogsStore, sensitiveUrlMasker),
        )
    }

    private fun OkHttpClient.Builder.applyWireMockRedirect(): OkHttpClient.Builder {
        return if (BuildConfig.MOCK_DATA_SOURCE) {
            addInterceptor(interceptor = WireMockRedirectInterceptor())
        } else {
            this
        }
    }

    private fun OkHttpClient.Builder.addLoggers(apiConfigId: ApiConfig.ID, context: Context): OkHttpClient.Builder {
        if (apiConfigId in excludedApiForLogging) return this

        return if (BuildConfig.LOG_ENABLED) {
            addInterceptor(interceptor = ChuckerInterceptor(context))
            addInterceptor(interceptor = createNetworkLoggingInterceptor())
        } else {
            this
        }
    }

    @Suppress("UseEmptyCounterpart")
    private companion object {

        val excludedApiForLogging: Set<ApiConfig.ID> = setOf(
            // StakeKit.ID,
            MoonPay.ID,
        )
    }
}