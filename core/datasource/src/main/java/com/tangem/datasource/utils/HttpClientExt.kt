package com.tangem.datasource.utils

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.SwitchEnvironmentInterceptor
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.api.utils.ConnectTimeout
import com.tangem.datasource.api.utils.ReadTimeout
import com.tangem.datasource.api.utils.WriteTimeout
import com.tangem.utils.Provider
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Invocation

/** Extension for adding headers [requestHeaders] to every [OkHttpClient] request */
internal fun OkHttpClient.Builder.addHeaders(vararg requestHeaders: RequestHeader): OkHttpClient.Builder {
    return addInterceptor(
        Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                requestHeaders
                    .flatMap { it.values.toList() }
                    .forEach { addHeader(it.first, it.second.invoke()) }
            }.build()

            chain.proceed(request)
        },
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

            chain
                .apply {
                    connectionTimeout?.let { withConnectTimeout(timeout = it.duration, unit = it.unit) }
                }
                .apply {
                    readTimeout?.let { withReadTimeout(timeout = it.duration, unit = it.unit) }
                }
                .apply {
                    writeTimeout?.let { withWriteTimeout(timeout = it.duration, unit = it.unit) }
                }
                .proceed(request)
        },
    )
}

/** Extension for adding headers [requestHeaders] to every [OkHttpClient] request */
internal fun OkHttpClient.Builder.addHeaders(requestHeaders: Map<String, Provider<String>>): OkHttpClient.Builder {
    return addInterceptor(
        Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                requestHeaders.forEach { addHeader(it.key, it.value.invoke()) }
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
 * Add environment switcher
 *
 * @param id                class of [ApiConfig]
 * @param apiConfigsManager api configs manager
 */
internal fun OkHttpClient.Builder.addEnvironmentSwitcher(
    id: ApiConfig.ID,
    apiConfigsManager: ApiConfigsManager,
): OkHttpClient.Builder {
    return if (BuildConfig.TESTER_MENU_ENABLED) {
        addInterceptor(
            interceptor = SwitchEnvironmentInterceptor(
                id = id,
                apiConfigsManager = apiConfigsManager,
            ),
        )
    } else {
        this
    }
}