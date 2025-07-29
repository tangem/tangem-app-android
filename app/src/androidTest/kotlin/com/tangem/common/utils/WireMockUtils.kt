package com.tangem.common.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException

/**
 * Method uses to set WireMock scenario state
 */
fun setWireMockScenarioState(
    scenarioName: String,
    state: String,
    baseUrl: String = "[REDACTED_ENV_URL]"
): Boolean {
    val client = OkHttpClient()
    val json = """{"state": "$state"}"""
    val mediaType = "application/json".toMediaType()

    val request = Request.Builder()
        .url("$baseUrl/__admin/scenarios/$scenarioName/state")
        .put(json.toRequestBody(mediaType))
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            Timber.d("WireMock scenario request URL: ${request.url}")
            Timber.d("WireMock scenario request body: $json")
            Timber.d("WireMock scenario response: ${response.code} - ${response.message}")
            Timber.d("WireMock scenario response body: $body")
            response.isSuccessful
        }
    } catch (e: IOException) {
        Timber.e(e, "WireMock scenario error")
        false
    }
}

/**
 * Method checks accessibility of WireMock
 */
fun checkWireMockStatus(baseUrl: String = "[REDACTED_ENV_URL]"): Boolean {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$baseUrl/__admin/scenarios")
        .get()
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            Timber.d("WireMock status check: ${response.code}")
            Timber.d("Available scenarios: $body")
            response.isSuccessful
        }
    } catch (e: IOException) {
        Timber.e(e, "WireMock not accessible")
        false
    }
}

/**
 * Method to reset all WireMock scenarios
 */
fun resetWireMockScenarios(baseUrl: String = "[REDACTED_ENV_URL]"): Boolean {
    Timber.i("=== WireMock Scenarios Reset ===")
    Timber.i("Base URL: $baseUrl")

    val client = OkHttpClient()
    val url = "$baseUrl/__admin/scenarios/reset"
    Timber.i("Request URL: $url")

    val request = Request.Builder()
        .url(url)
        .post("".toRequestBody())
        .build()

    return try {
        Timber.d("Sending reset request...")
        client.newCall(request).execute().use { response ->
            Timber.d("Response code: ${response.code}")
            Timber.d("Response message: ${response.message}")
            val responseBody = response.body?.string() ?: ""
            Timber.d("Response body: $responseBody")

            val isSuccessful = response.isSuccessful
            Timber.d("Is successful: $isSuccessful")
            isSuccessful
        }
    } catch (e: IOException) {
        Timber.e(e, "Exception during reset")
        false
    }
}