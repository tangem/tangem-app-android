package com.tangem.datasource.utils

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * OkHttp interceptor that redirects requests from wiremock.tests-d.com to a local WireMock instance.
 *
 * This interceptor is used for CI testing where each emulator runs its own local WireMock server.
 * When [overrideBaseUrl] is set (via instrumentation arguments), all requests to wiremock.tests-d.com
 * are redirected to the specified local URL.
 *
 * Usage:
 * - Local development: [overrideBaseUrl] is null, requests go to wiremock.tests-d.com as usual
 * - CI with local WireMock: [overrideBaseUrl] = "http://10.0.2.2:8080", requests are redirected
 *
[REDACTED_AUTHOR]
 */
class WireMockRedirectInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val override = overrideBaseUrl ?: return chain.proceed(chain.request())

        val request = chain.request()
        val url = request.url.toString()

        if (url.contains(WIREMOCK_REMOTE_HOST)) {
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
        private const val WIREMOCK_REMOTE_HOST = "wiremock.tests-d.com"
        private const val WIREMOCK_REMOTE_URL = "[REDACTED_ENV_URL]"

        /**
         * Override base URL for WireMock requests.
         * When null (default), requests go to wiremock.tests-d.com.
         * When set (e.g., "http://localhost:8080"), requests are redirected to local WireMock.
         *
         * IMPORTANT: Use "http://localhost:8080" (not 10.0.2.2) because adb reverse
         * only intercepts connections to localhost. When using adb reverse tcp:8080 tcp:808X,
         * connections to localhost:8080 on the emulator are forwarded to the host.
         *
         * Set this value from test setup using instrumentation arguments:
         * ```
         * WireMockRedirectInterceptor.overrideBaseUrl =
         *     InstrumentationRegistry.getArguments().getString("wiremockBaseUrl")
         * ```
         */
        @Volatile
        var overrideBaseUrl: String? = null
    }
}