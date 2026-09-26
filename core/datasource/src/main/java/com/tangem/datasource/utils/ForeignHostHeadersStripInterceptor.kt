package com.tangem.datasource.utils

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Network-level interceptor that removes the API's configured headers (`api-key`, `X-API-KEY`, card identifiers, …)
 * from any request that is not addressed to the API's own host.
 *
 * The headers are attached by an application interceptor once per call; OkHttp then follows redirects itself and,
 * for a redirect to another host, only drops `Authorization`. Every other header — including static API keys —
 * would travel to the foreign host. Installed as a network interceptor, this runs for every hop.
 */
internal class ForeignHostHeadersStripInterceptor(
    private val apiHost: String,
    private val headerNames: Set<String>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (headerNames.isEmpty() || request.url.host.equals(apiHost, ignoreCase = true)) {
            return chain.proceed(request)
        }
        val stripped = request.newBuilder().apply {
            headerNames.forEach(::removeHeader)
        }.build()
        return chain.proceed(stripped)
    }
}
