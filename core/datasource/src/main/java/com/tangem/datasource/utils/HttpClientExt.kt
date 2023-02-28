package com.tangem.datasource.utils

import com.tangem.datasource.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level

/** Extension for adding headers [requestHeaders] to every [OkHttpClient] request */
internal fun OkHttpClient.Builder.addHeaders(vararg requestHeaders: RequestHeader): OkHttpClient.Builder {
    return addInterceptor(
        Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                requestHeaders
                    .flatMap(RequestHeader::values)
                    .forEach { addHeader(it.first, it.second) }
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
internal fun OkHttpClient.Builder.allowLogging(level: Level = Level.BODY): OkHttpClient.Builder =
    if (BuildConfig.DEBUG) addInterceptor(interceptor = HttpLoggingInterceptor().setLevel(level)) else this
