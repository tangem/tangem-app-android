package com.tangem.datasource.utils

import com.tangem.utils.ProviderSuspend
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/** Extension for adding headers [requestHeaders] to every [OkHttpClient] request */
internal fun OkHttpClient.Builder.addHeaders(vararg requestHeaders: RequestHeader): OkHttpClient.Builder {
    return addInterceptor(
        interceptor = AddHeadersInterceptor(requestHeaders = requestHeaders.toSet()),
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