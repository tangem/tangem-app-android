package com.tangem.datasource.utils

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
    private val requestHeaders: Set<RequestHeader>,
) : Interceptor {

    constructor(requestHeader: RequestHeader) : this(requestHeaders = setOf(requestHeader))

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        requestHeaders
            .flatMap { it.values.toList() }
            .forEach { (name, valueProvider) ->
                val value = runBlocking { valueProvider() }

                requestBuilder.addHeader(name = name, value = value)
            }

        val request = requestBuilder.build()

        return chain.proceed(request)
    }
}