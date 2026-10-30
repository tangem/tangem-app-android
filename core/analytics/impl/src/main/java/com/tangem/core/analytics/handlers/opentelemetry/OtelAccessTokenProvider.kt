package com.tangem.core.analytics.handlers.opentelemetry

/**
 * Supplies the device access token that authorizes OTLP metric uploads, or `null` while the device has no
 * backend session — the metrics then stay in the on-disk buffer until a token is available.
 */
fun interface OtelAccessTokenProvider {

    suspend fun getAccessToken(): String?
}