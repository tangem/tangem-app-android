package com.tangem.datasource.di.utils

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.SwitchEnvironmentInterceptor
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfigs
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.api.common.response.ApiResponseCallAdapterFactory
import com.tangem.datasource.api.utils.ConnectTimeout
import com.tangem.datasource.api.utils.ReadTimeout
import com.tangem.datasource.api.utils.WriteTimeout
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.utils.NetworkLogsSaveInterceptor
import com.tangem.datasource.utils.addHeaders
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Invocation
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
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
@Singleton
internal class RetrofitApiBuilder @Inject constructor(
    private val apiConfigs: ApiConfigs,
    private val apiConfigsManager: ApiConfigsManager,
    @NetworkMoshi private val moshi: Moshi,
    private val analyticsErrorHandler: AnalyticsErrorHandler,
    @ApplicationContext private val context: Context,
    private val appLogsStore: AppLogsStore,
) {

    private val configsBaseUrls: Map<ApiConfig.ID, Set<String>> = getConfigsBaseUrls()

    /**
     * Builds a Retrofit API instance for the specified API configuration ID
     *
     * @param apiConfigId             the ID of the API configuration to use
     * @param applyTimeoutAnnotations whether to apply timeout annotations to the requests. See [ReadTimeout], etc.
     * @param timeouts                optional timeouts for the requests
     * @param logsSaving              whether to enable logs saving
     *
     * @return an instance [T] of the specified API interface
     */
    inline fun <reified T> build(
        apiConfigId: ApiConfig.ID,
        applyTimeoutAnnotations: Boolean,
        timeouts: Timeouts? = null,
        logsSaving: Boolean = true,
    ): T {
        val environmentConfig = apiConfigsManager.getEnvironmentConfig(apiConfigId)

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create(analyticsErrorHandler))
            .baseUrl(environmentConfig.baseUrl)
            .client(
                OkHttpClient.Builder()
                    .applyApiConfig(apiConfigId = apiConfigId, environmentConfig = environmentConfig)
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
            .create(T::class.java)
    }

    data class Timeouts(
        val callTimeoutSeconds: Long? = null,
        val connectTimeoutSeconds: Long? = null,
        val readTimeoutSeconds: Long? = null,
        val writeTimeoutSeconds: Long? = null,
    )

    private fun getConfigsBaseUrls(): Map<ApiConfig.ID, Set<String>> {
        return apiConfigs.associate { config ->
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
            interceptor = NetworkLogsSaveInterceptor(appLogsStore),
        )
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

    private companion object {

        val excludedApiForLogging: Set<ApiConfig.ID> = setOf(
            // ApiConfig.ID.StakeKit,
        )
    }
}