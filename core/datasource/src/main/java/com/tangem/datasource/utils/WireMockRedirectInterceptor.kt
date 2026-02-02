package com.tangem.datasource.utils

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * OkHttp interceptor that redirects requests from wiremock.tests-d.com to a local WireMock instance.
 */
class WireMockRedirectInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val override = overriddenBaseUrl ?: return chain.proceed(chain.request())

        val request = chain.request()
        val url = request.url.toString()

        if (url.contains(WIREMOCK_REMOTE_URL)) {
            val newUrl = url.replace(WIREMOCK_REMOTE_URL, override.trimEnd('/'))
            Timber.d("WireMockRedirect: $url -> $newUrl")
            val newRequest = request.newBuilder()
                .url(newUrl)
                .build()
            return chain.proceed(newRequest)
        }

        return chain.proceed(request)
    }

    companion object {
        private const val WIREMOCK_REMOTE_URL = "[REDACTED_ENV_URL]"

        /**
         * Override base URL for WireMock requests.
         * When null (default), requests go to wiremock.tests-d.com.
         * When set (e.g., "http://localhost:8080"), requests are redirected to local WireMock instance.
         */
        var overriddenBaseUrl: String? = null
    }
}