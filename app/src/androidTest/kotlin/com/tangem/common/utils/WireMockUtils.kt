package com.tangem.common.utils

import com.tangem.datasource.utils.WireMockRedirectInterceptor
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.tangem.utils.logging.TangemLogger
import java.io.IOException

private const val DEFAULT_WIREMOCK_URL = "[REDACTED_ENV_URL]"

/**
 * Returns the WireMock base URL to use.
 */
private fun getWireMockBaseUrl(): String =
    WireMockRedirectInterceptor.overriddenBaseUrl ?: DEFAULT_WIREMOCK_URL

/**
 * Method uses to set WireMock scenario state
 * @param scenarioName Name of the scenario to modify
 * @param state The target state to set (must be one of the scenario's possibleStates)
 * @param baseUrl WireMock base URL (defaults to local override if set, otherwise remote)
 * @return true if state was set successfully, false otherwise
 */
fun setWireMockScenarioState(
    scenarioName: String,
    state: String,
    baseUrl: String = getWireMockBaseUrl()
): Boolean {
    TangemLogger.i("=== WireMock Scenario Set ===")
    TangemLogger.i("Setting scenario '$scenarioName' to state: $state")
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
            TangemLogger.d("WireMock scenario request URL: ${request.url}")
            TangemLogger.d("WireMock scenario request body: $json")
            TangemLogger.d("WireMock scenario response: ${response.code} - ${response.message}")
            TangemLogger.d("WireMock scenario response body: $body")
            response.isSuccessful
        }
    } catch (e: IOException) {
        TangemLogger.e("WireMock scenario error", e)
        false
    }
}

/**
 * Method checks accessibility of WireMock
 * @param baseUrl WireMock base URL (defaults to local override if set, otherwise remote)
 */
fun checkWireMockStatus(baseUrl: String = getWireMockBaseUrl()): Boolean {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$baseUrl/__admin/scenarios")
        .get()
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            TangemLogger.d("WireMock status check: ${response.code}")
            TangemLogger.d("Available scenarios: $body")
            response.isSuccessful
        }
    } catch (e: IOException) {
        TangemLogger.e("WireMock not accessible", e)
        false
    }
}

/**
 * Method to reset all WireMock scenarios
 * @param baseUrl WireMock base URL (defaults to local override if set, otherwise remote)
 */
fun resetWireMockScenarios(baseUrl: String = getWireMockBaseUrl()): Boolean {
    TangemLogger.i("=== WireMock Scenarios Reset ===")
    TangemLogger.i("Base URL: $baseUrl")

    val client = OkHttpClient()
    val url = "$baseUrl/__admin/scenarios/reset"
    TangemLogger.i("Request URL: $url")

    val request = Request.Builder()
        .url(url)
        .post("".toRequestBody())
        .build()

    return try {
        TangemLogger.d("Sending reset request...")
        client.newCall(request).execute().use { response ->
            TangemLogger.d("Response code: ${response.code}")
            TangemLogger.d("Response message: ${response.message}")
            val responseBody = response.body?.string() ?: ""
            TangemLogger.d("Response body: $responseBody")

            val isSuccessful = response.isSuccessful
            TangemLogger.d("Is successful: $isSuccessful")
            isSuccessful
        }
    } catch (e: IOException) {
        TangemLogger.e("Exception during reset", e)
        false
    }
}

/**
 * Method to reset a specific WireMock scenario to its initial state
 * @param scenarioName Name of the scenario to reset
 * @param initialState The target state to reset the scenario to (must be one of the scenario's possibleStates)
 * @param baseUrl WireMock base URL (defaults to local override if set, otherwise remote)
 * @return true if reset was successful, false otherwise
 */
fun resetWireMockScenarioState(
    scenarioName: String,
    initialState: String = "Started",
    baseUrl: String = getWireMockBaseUrl()
): Boolean {
    TangemLogger.i("=== WireMock Scenario Reset ===")
    TangemLogger.i("Resetting scenario '$scenarioName' to initial state: $initialState")
    return setWireMockScenarioState(scenarioName, initialState, baseUrl)
}