package com.tangem.common.utils

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import com.tangem.utils.logging.TangemLogger
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

// WC URIs embed a session symKey that lets anyone join/hijack the session — strip it before logging.
private val WC_SECRET_REGEX = Regex("(symKey(?:=|%3D))[^&\\s\"']+", RegexOption.IGNORE_CASE)

private fun redactWcSecrets(text: String): String =
    WC_SECRET_REGEX.replace(text) { "${it.groupValues[1]}<redacted>" }

private const val DEFAULT_MAX_ATTEMPTS = 4
private const val INITIAL_BACKOFF_MS = 500L

/**
 * Runs [block] with exponential backoff. Retries ONLY when [block] throws (transient failure:
 * network error, non-2xx, malformed body). A `null` return is treated as terminal (e.g. "200 but no
 * data for this key") and is NOT retried. Returns the block result, or null if all attempts failed.
 */
private fun <T> retryWithBackoff(
    maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    initialDelayMs: Long = INITIAL_BACKOFF_MS,
    block: (attempt: Int) -> T,
): T? {
    var delayMs = initialDelayMs
    var lastError: Throwable? = null
    repeat(maxAttempts) { i ->
        val attempt = i + 1
        try {
            return block(attempt)
        } catch (e: Exception) {
            lastError = e
            TangemLogger.w("Attempt $attempt/$maxAttempts failed: ${e.message}")
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(delayMs)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    TangemLogger.w("Retry backoff sleep interrupted; aborting retries")
                    return null
                }
                delayMs *= 2
            }
        }
    }
    TangemLogger.e("All $maxAttempts attempts failed", lastError)
    return null
}

/**
 * Logs the actual connected endpoint (IPv4/IPv6) and whether a proxy is in the path. Lets CI logs
 * distinguish "went out via the wrong egress / IPv6 / through a local proxy" from other failures.
 */
private val diagnosticEventListener = object : EventListener() {
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        TangemLogger.i("Connecting to ${inetSocketAddress.address?.hostAddress} (proxy=$proxy)")
    }
}

private fun diagnosticClient(connectSec: Long, readSec: Long, callSec: Long): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(connectSec, TimeUnit.SECONDS)
        .readTimeout(readSec, TimeUnit.SECONDS)
        .callTimeout(callSec, TimeUnit.SECONDS)
        // Don't follow redirects: a Cloudflare Access 302 must stay visible (its `location` points at
        // cloudflareaccess.com) so logHttpFailure can flag "egress IP not allow-listed". /health and
        // /addresses have no legitimate redirects.
        .followRedirects(false)
        .followSslRedirects(false)
        .eventListener(diagnosticEventListener)
        .build()

/**
 * Logs enough to classify a failed response at a glance: `cf-ray`/`location`/`server` reveal a
 * Cloudflare Access 302 (egress IP not allow-listed) vs an origin 5xx vs anything else.
 */
private fun logHttpFailure(tag: String, response: Response, body: String) {
    TangemLogger.e(
        "$tag failed: code=${response.code} cf-ray=${response.header("cf-ray") ?: "-"} " +
            "server=${response.header("server") ?: "-"} location=${response.header("location") ?: "-"} " +
            "body=${body.take(200)}",
    )
}

/**
 * Requests a WalletConnect URI from the qa-tools service.
 *
 * Response shape (see qa-tools `/wc_uri` swagger):
 *  - 200: { success: true, wcUri: "wc:...", network, wallet, tangemDeepLink, timestamp, processingTime }
 *  - 5xx: { error, network, timestamp, errorType }
 */
fun getWcUri(
    network: String = "ethereum",
    dAppUrl: String? = null,
    dAppName: String? = null,
    baseUrl: String = "[REDACTED_ENV_URL]"
): String? {
    val url = "$baseUrl/wc_uri".toHttpUrl().newBuilder()
        .addQueryParameter("network", network)
        .apply {
            if (dAppUrl != null) addQueryParameter("dappUrl", dAppUrl)
            if (dAppName != null) addQueryParameter("dappName", dAppName)
        }
        .build()
        .toString()
    TangemLogger.i("getWcUri: requesting $url")

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .get()
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val contentType = response.header("Content-Type") ?: "<missing>"
            TangemLogger.i(
                "getWcUri: HTTP ${response.code} ${response.message}, " +
                    "Content-Type=$contentType, body.length=${body.length}"
            )
            TangemLogger.i("getWcUri: raw body=${redactWcSecrets(body)}")

            if (!response.isSuccessful) {
                TangemLogger.e("getWcUri: non-2xx response (${response.code}), body=${redactWcSecrets(body)}")
                return@use null
            }

            if (body.isBlank()) {
                TangemLogger.e("getWcUri: response body is empty")
                return@use null
            }

            if (contentType.contains("text/html", ignoreCase = true) ||
                body.trimStart().startsWith("<")
            ) {
                val server = response.header("Server").orEmpty()
                val wwwAuth = response.header("WWW-Authenticate").orEmpty()
                val isCloudflareAccess = server.contains("cloudflare", ignoreCase = true) ||
                    wwwAuth.contains("Cloudflare-Access", ignoreCase = true) ||
                    body.contains("cloudflareaccess.com", ignoreCase = true)
                if (isCloudflareAccess) {
                    TangemLogger.e(
                        "getWcUri: blocked by Cloudflare Access. The test runner is not " +
                            "authorized to reach $url — connect to the corporate VPN or " +
                            "configure a Cloudflare Access service token (CF-Access-Client-Id / " +
                            "CF-Access-Client-Secret headers) on the device."
                    )
                } else {
                    TangemLogger.e(
                        "getWcUri: server returned HTML instead of JSON for $url. " +
                            "Verify [REDACTED_ENV_URL] and the current API in /docs."
                    )
                }
                return@use null
            }

            val jsonObject = try {
                JSONObject(body)
            } catch (e: Exception) {
                TangemLogger.e("getWcUri: failed to parse body as JSON: ${redactWcSecrets(body)}", e)
                return@use null
            }

            TangemLogger.i("getWcUri: response keys=${jsonObject.keys().asSequence().toList()}")

            val success = jsonObject.optBoolean("success", false)
            if (!success) {
                val error = jsonObject.optString("error", "<no error field>")
                val errorType = jsonObject.optString("errorType", "<no errorType field>")
                TangemLogger.e(
                    "getWcUri: API success=false, error=$error, errorType=$errorType, body=${redactWcSecrets(body)}"
                )
                return@use null
            }

            val wcUri = jsonObject.optString("wcUri", "")
            val tangemDeepLink = jsonObject.optString("tangemDeepLink", "")
            val processingTime = jsonObject.optString("processingTime", "")
            TangemLogger.i(
                "getWcUri: parsed wcUri=${redactWcSecrets(wcUri)}, " +
                    "tangemDeepLink=${redactWcSecrets(tangemDeepLink)}, processingTime=$processingTime"
            )

            if (wcUri.isBlank() || !wcUri.startsWith("wc:")) {
                TangemLogger.e(
                    "getWcUri: wcUri is missing or has unexpected format. " +
                        "wcUri='${redactWcSecrets(wcUri)}', body=${redactWcSecrets(body)}"
                )
                return@use null
            }

            wcUri
        }
    } catch (e: Exception) {
        TangemLogger.e("getWcUri: exception while requesting $url", e)
        null
    }
}

fun getAddressesFromApi(
    seedKey: String,
    baseUrl: String = "[REDACTED_ENV_URL]",
): String? {
    TangemLogger.i("Getting addresses for seed key: $seedKey")

    val client = diagnosticClient(connectSec = 30, readSec = 60, callSec = 90)
    val request = Request.Builder()
        .url("$baseUrl/addresses")
        .get()
        .build()

    return retryWithBackoff { attempt ->
        TangemLogger.i("Getting addresses for '$seedKey', attempt $attempt")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // Transient (network/Access 302/5xx) — throw so retryWithBackoff retries.
                logHttpFailure("getAddressesFromApi", response, response.body?.string() ?: "")
                throw IOException("getAddressesFromApi: HTTP ${response.code}")
            }

            val body = response.body?.string() ?: ""
            val contentType = response.header("Content-Type") ?: ""
            if (!contentType.contains("application/json") && !body.trimStart().startsWith("{")) {
                logHttpFailure("getAddressesFromApi (not JSON)", response, body)
                throw IOException("getAddressesFromApi: unexpected non-JSON response")
            }

            val jsonObject = JSONObject(body)
            val data = jsonObject.optJSONObject("data") ?: jsonObject
            val seedData = data.optJSONArray(seedKey)
            if (seedData != null) {
                TangemLogger.i("Got addresses for $seedKey: ${seedData.length()} entries")
                seedData.toString()
            } else {
                // Terminal: server responded fine but has no data for this key — retrying won't help.
                TangemLogger.e("No data found for seed key: $seedKey")
                null
            }
        }
    }
}

fun checkServiceHealth(
    baseUrl: String = "[REDACTED_ENV_URL]"
): String? {
    TangemLogger.i("Checking service health")

    val client = diagnosticClient(connectSec = 15, readSec = 30, callSec = 45)
    val request = Request.Builder()
        .url("$baseUrl/health")
        .get()
        .build()

    return retryWithBackoff { attempt ->
        TangemLogger.i("Checking service health, attempt $attempt")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // Transient (network/Access 302/5xx) — throw so retryWithBackoff retries.
                logHttpFailure("checkServiceHealth", response, response.body?.string() ?: "")
                throw IOException("checkServiceHealth: HTTP ${response.code}")
            }

            val body = response.body?.string() ?: ""
            val status = if (body.isEmpty()) "" else JSONObject(body).optString("status", "")
            if (status.isNotEmpty()) {
                TangemLogger.i("Got status successfully: $status")
                status
            } else {
                // Empty/malformed body from a 2xx — treat as transient and retry.
                logHttpFailure("checkServiceHealth (empty status)", response, body)
                throw IOException("checkServiceHealth: missing 'status' field")
            }
        }
    }
}