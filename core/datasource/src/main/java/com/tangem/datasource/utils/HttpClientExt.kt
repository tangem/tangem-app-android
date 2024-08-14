package com.tangem.datasource.utils

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.SwitchEnvironmentInterceptor
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.utils.Provider
import okhttp3.Interceptor
import okhttp3.OkHttpClient

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