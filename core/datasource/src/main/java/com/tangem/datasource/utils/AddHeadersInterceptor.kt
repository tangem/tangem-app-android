package com.tangem.datasource.utils

import com.tangem.utils.ProviderSuspend
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptors for adding headers
 *
 * @property requestHeaders request headers
 *
[REDACTED_AUTHOR]
 */
class AddHeadersInterceptor(
    private val requestHeaders: Map<String, ProviderSuspend<String>>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        requestHeaders.forEach { (name, valueProvider) ->
            val value = runBlocking { valueProvider() }

            requestBuilder.addHeader(name = name, value = value)
        }

        val request = requestBuilder.build()

        return chain.proceed(request)
    }
}