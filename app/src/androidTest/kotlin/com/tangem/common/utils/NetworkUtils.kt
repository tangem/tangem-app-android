package com.tangem.common.utils

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.tangem.utils.logging.TangemLogger
import java.util.concurrent.TimeUnit

// WC URIs embed a session symKey that lets anyone join/hijack the session — strip it before logging.
private val WC_SECRET_REGEX = Regex("(symKey(?:=|%3D))[^&\\s\"']+", RegexOption.IGNORE_CASE)

private fun redactWcSecrets(text: String): String =
    WC_SECRET_REGEX.replace(text) { "${it.groupValues[1]}<redacted>" }

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

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url("$baseUrl/addresses")
        .get()
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            TangemLogger.i("Response code: ${response.code}")

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""

                val contentType = response.header("Content-Type") ?: ""
                if (!contentType.contains("application/json") && !body.trimStart().startsWith("{")) {
                    TangemLogger.e("Unexpected response (not JSON), Content-Type: $contentType, body: $body")
                    return null
                }

                val jsonObject = JSONObject(body)
                val data = jsonObject.optJSONObject("data") ?: jsonObject
                val seedData = data.optJSONArray(seedKey)

                if (seedData != null) {
                    TangemLogger.i("Got addresses for $seedKey: ${seedData.length()} entries")
                    seedData.toString()
                } else {
                    TangemLogger.e("No data found for seed key: $seedKey")
                    null
                }
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                TangemLogger.e("Request failed: ${response.code}, body: $errorBody")
                null
            }
        }
    } catch (e: Exception) {
        TangemLogger.e("Error getting addresses", e)
        null
    }
}

fun checkServiceHealth(
    baseUrl: String = "[REDACTED_ENV_URL]"
): String? {
    TangemLogger.i("Checking service health")

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$baseUrl/health")
        .get()
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            TangemLogger.i("Response code: ${response.code}")

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                TangemLogger.i("Response body: $body")

                if (body.isEmpty()) {
                    TangemLogger.e("Response body is empty")
                    return null
                }

                val jsonObject = JSONObject(body)
                val status = jsonObject.optString("status", "")

                if (status.isNotEmpty()) {
                    TangemLogger.i("Got status successfully: $status")
                    status
                } else {
                    TangemLogger.e("Status field is missing or empty")
                    null
                }
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                TangemLogger.e("Request failed: ${response.code}, body: $errorBody")
                null
            }
        }
    } catch (e: Exception) {
        TangemLogger.e("Error checking health", e)
        null
    }
}