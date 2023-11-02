package com.tangem.datasource.utils

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/** Extension for adding headers [requestHeaders] to every [OkHttpClient] request */
internal fun OkHttpClient.Builder.addHeaders(vararg requestHeaders: RequestHeader): OkHttpClient.Builder {
    return addInterceptor(
        Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                requestHeaders
                    .flatMap(RequestHeader::values)
                    .forEach { addHeader(it.first, it.second.invoke()) }
            }.build()

            chain.proceed(request)
        },
    )
}

/**
 * Extension for logging each [OkHttpClient] request
 *
 * @param level logging level. By default, only the request body.
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