package com.tangem.datasource.utils

import com.tangem.utils.logging.TangemLogger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that redirects requests from wiremock.tests-d.com to a local WireMock instance.
 */
class WireMockRedirectInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val override = overriddenBaseUrl ?: return chain.proceed(chain.request())

        val request = chain.request()
        val url = request.url.toString()
        val host = request.url.host
        val sanitizedOverride = override.trimEnd('/')

        if (host == WIREMOCK_REMOTE_HOST) {
            val newUrl = url.replace(WIREMOCK_REMOTE_URL, sanitizedOverride)
            TangemLogger.d("WireMockRedirect: $url -> $newUrl")
            return chain.proceed(request.newBuilder().url(newUrl).build())
        }

        if (host in REDIRECTABLE_THIRD_PARTY_HOSTS) {
            val newUrl = url.replace("https://$host", "$sanitizedOverride/$host")
            TangemLogger.d("WireMockRedirect (3p): $url -> $newUrl")
            return chain.proceed(request.newBuilder().url(newUrl).build())
        }

        return chain.proceed(request)
    }

    companion object {
        private const val WIREMOCK_REMOTE_HOST = "wiremock.tests-d.com"
        private const val WIREMOCK_REMOTE_URL = "https://$WIREMOCK_REMOTE_HOST"

        /**
         * Upstream hosts that have no other override knob and should be funnelled into WireMock
         * when [overriddenBaseUrl] is set. Each matched URL becomes `<override>/<host>/<original-path>`,
         * so mock mappings should live under that host-prefixed path in tangem-api-mocks. Matching
         * is done against the request's parsed host (exact equality) — substring matching would
         * incorrectly redirect look-alikes such as `deep-index.moralis.io.evil.example`.
         */
        private val REDIRECTABLE_THIRD_PARTY_HOSTS = setOf(
            "deep-index.moralis.io",
            "solana-gateway.moralis.io",
        )

        /**
         * Override base URL for WireMock requests.
         * When null (default), requests go to wiremock.tests-d.com.
         * When set (e.g., "http://localhost:8080"), requests are redirected to local WireMock instance.
         */
        var overriddenBaseUrl: String? = null
    }
}