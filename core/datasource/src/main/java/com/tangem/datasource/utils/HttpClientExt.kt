package com.tangem.datasource.utils

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.SwitchEnvironmentInterceptor
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.api.utils.ConnectTimeout
import com.tangem.datasource.api.utils.ReadTimeout
import com.tangem.datasource.api.utils.WriteTimeout
import com.tangem.utils.ProviderSuspend
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Invocation

/** Extension for adding headers [requestHeaders] to every [OkHttpClient] request */
internal fun OkHttpClient.Builder.addHeaders(vararg requestHeaders: RequestHeader): OkHttpClient.Builder {
    return addInterceptor(
        interceptor = AddHeadersInterceptor(requestHeaders = requestHeaders.toSet()),
    )
}

/**
 * Apply timeout annotations [Interceptor].
 * Add this [Interceptor] to [OkHttpClient] if use timeout annotations for retrofit requests.
 */
internal fun OkHttpClient.Builder.applyTimeoutAnnotations(): OkHttpClient.Builder {
    return addInterceptor(
        Interceptor { chain ->
            val request = chain.request()
            val tag = request.tag(Invocation::class.java)
            val connectionTimeout = tag?.method()?.getAnnotation(ConnectTimeout::class.java)
            val readTimeout = tag?.method()?.getAnnotation(ReadTimeout::class.java)
            val writeTimeout = tag?.method()?.getAnnotation(WriteTimeout::class.java)

            chain.run {
                connectionTimeout?.let { withConnectTimeout(timeout = it.duration, unit = it.unit) } ?: this
            }.run {
                readTimeout?.let { withReadTimeout(timeout = it.duration, unit = it.unit) } ?: this
            }.run {
                writeTimeout?.let { withWriteTimeout(timeout = it.duration, unit = it.unit) } ?: this
            }.proceed(request)
        },
    )
}

/** Extension for adding headers [requestHeaders] to every [OkHttpClient] request */
internal fun OkHttpClient.Builder.addHeaders(
    requestHeaders: Map<String, ProviderSuspend<String>>,
): OkHttpClient.Builder {
    return addInterceptor(
        Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                runBlocking {
                    requestHeaders.forEach {
                        val value = it.value.invoke()
                        if (value.isNotBlank()) addHeader(name = it.key, value = value)
                    }
                }
            }.build()

            chain.proceed(request)
        },
    )
}

/**
 * Extension for logging each [OkHttpClient] request
 *
 * @param context context
 */
internal fun OkHttpClient.Builder.addLoggers(context: Context? = null): OkHttpClient.Builder {
    return if (BuildConfig.LOG_ENABLED) {
        context?.let {
            addInterceptor(interceptor = ChuckerInterceptor(it))
        }
        addInterceptor(interceptor = createNetworkLoggingInterceptor())
    } else {
        this
    }
}

/**
 * Apply api config
 *
 * @param id                class of [ApiConfig]
 * @param apiConfigsManager api configs manager
 */
internal fun OkHttpClient.Builder.applyApiConfig(
    id: ApiConfig.ID,
    apiConfigsManager: ApiConfigsManager,
): OkHttpClient.Builder {
    return if (BuildConfig.TESTER_MENU_ENABLED || BuildConfig.BUILD_TYPE == MOCKED_BUILD_TYPE) {
        addInterceptor(
            interceptor = SwitchEnvironmentInterceptor(
                id = id,
                apiConfigsManager = apiConfigsManager,
            ),
        )
    } else {
        val headers = apiConfigsManager.getEnvironmentConfig(id).headers

        this.addHeaders(headers)
    }
}