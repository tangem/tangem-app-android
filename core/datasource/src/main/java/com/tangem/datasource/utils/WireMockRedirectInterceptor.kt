package com.tangem.datasource.utils

import com.tangem.utils.logging.TangemLogger
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp interceptor that redirects requests from wiremock.tests-d.com to a local WireMock instance.
 *
 * When a request hits the tangem-api-mocks catch-all (an unmocked endpoint → HTTP 404 with the
 * [MOCKED_ENDPOINT_NOT_CONFIGURED_MARKER] body), it folds the failing endpoint (method + URL) into the
 * response body. That body becomes [com.tangem.datasource.api.common.response.ApiResponseError.HttpException.errorBody]
 * (see ResponseExt), whose data-class `toString()` is what appears in a failing test's stack trace — so the
 * exact missing mapping is named instead of an opaque `HttpException(404, errorBody={"error":"not_found",…})`.
 */
class WireMockRedirectInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val override = overriddenBaseUrl ?: return chain.proceed(chain.request())

        val request = chain.request()
        val url = request.url.toString()
        val host = request.url.host
        val sanitizedOverride = override.trimEnd('/')

        val redirectedRequest = when {
            host == WIREMOCK_REMOTE_HOST -> {
                val newUrl = url.replace(WIREMOCK_REMOTE_URL, sanitizedOverride)
                TangemLogger.d("WireMockRedirect: $url -> $newUrl")
                request.newBuilder().url(newUrl).build()
            }
            host in REDIRECTABLE_THIRD_PARTY_HOSTS -> {
                val newUrl = url.replace("https://$host", "$sanitizedOverride/$host")
                TangemLogger.d("WireMockRedirect (3p): $url -> $newUrl")
                request.newBuilder().url(newUrl).build()
            }
            else -> request
        }

        val response = chain.proceed(redirectedRequest)
        return response.withEndpointInMockNotConfiguredError(redirectedRequest)
    }

    /**
     * If [this] is the tangem-api-mocks catch-all 404 ("Mocked endpoint not configured"), rewrite the error body
     * to also carry the failing endpoint, so it surfaces in the resulting `HttpException.errorBody`. Non-404
     * responses (incl. the POST JSON-RPC "unreachable" catch-all, which is handled gracefully and must not be
     * altered) are returned untouched.
     */
    private fun Response.withEndpointInMockNotConfiguredError(request: Request): Response {
        if (code != HTTP_NOT_FOUND) return this

        val original = runCatching { peekBody(PEEK_LIMIT_BYTES).string() }.getOrNull() ?: return this
        if (!original.contains(MOCKED_ENDPOINT_NOT_CONFIGURED_MARKER)) return this

        val endpoint = "${request.method} ${request.url}".jsonEscaped()
        val enrichedJson = original.indexOf('{').let { open ->
            if (open < 0) {
                """{"message":"$MOCKED_ENDPOINT_NOT_CONFIGURED_MARKER","endpoint":"$endpoint"}"""
            } else {
                original.substring(0, open + 1) + """"endpoint":"$endpoint",""" + original.substring(open + 1)
            }
        }

        val contentType = body?.contentType()
        body?.close()
        return newBuilder().body(enrichedJson.toResponseBody(contentType)).build()
    }

    private fun String.jsonEscaped(): String = replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        private const val WIREMOCK_REMOTE_HOST = "wiremock.tests-d.com"
        private const val WIREMOCK_REMOTE_URL = "https://$WIREMOCK_REMOTE_HOST"

        private const val HTTP_NOT_FOUND = 404

        /** Bounded peek so a large real response is never fully buffered just to look for the marker. */
        private const val PEEK_LIMIT_BYTES = 4_096L

        /** Emitted by the tangem-api-mocks catch-all mapping for any endpoint without an explicit mapping. */
        private const val MOCKED_ENDPOINT_NOT_CONFIGURED_MARKER = "Mocked endpoint not configured"

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
            "api.etherscan.io",
            "eth-blockbook.nownodes.io",
        )

        /**
         * Override base URL for WireMock requests.
         * When null (default), requests go to wiremock.tests-d.com.
         * When set (e.g., "http://localhost:8080"), requests are redirected to local WireMock instance.
         */
        var overriddenBaseUrl: String? = null
    }
}