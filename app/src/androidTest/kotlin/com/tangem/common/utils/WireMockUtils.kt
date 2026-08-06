package com.tangem.common.utils

import com.tangem.datasource.utils.WireMockRedirectInterceptor
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
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
    val json = JSONObject().put("state", state).toString()
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
 * Counts requests matching [method] and [urlPathPattern] in the WireMock journal (`/__admin/requests/count`).
 * @param method HTTP method to match (e.g. "POST")
 * @param urlPathPattern Regex matched against the request URL path (e.g. "/v1/exchange-sent")
 * @param baseUrl WireMock base URL (defaults to local override if set, otherwise remote)
 * @return number of matching requests, or 0 if the journal could not be queried
 */
fun getWireMockRequestCount(
    method: String,
    urlPathPattern: String,
    baseUrl: String = getWireMockBaseUrl(),
): Int {
    val client = OkHttpClient()
    // Build via JSONObject so a regex urlPathPattern with quotes/backslashes stays valid JSON.
    val json = JSONObject()
        .put("method", method)
        .put("urlPathPattern", urlPathPattern)
        .toString()
    val mediaType = "application/json".toMediaType()

    val request = Request.Builder()
        .url("$baseUrl/__admin/requests/count")
        .post(json.toRequestBody(mediaType))
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            TangemLogger.d("WireMock request count response: ${response.code} - $body")
            if (response.isSuccessful) JSONObject(body).getInt("count") else 0
        }
    } catch (e: IOException) {
        TangemLogger.e("WireMock request count error", e)
        0
    } catch (e: JSONException) {
        TangemLogger.e("WireMock request count: unexpected response body", e)
        0
    }
}

/**
 * Counts requests matching [method], [urlPath] and an exact query parameter value in the WireMock
 * journal (`/__admin/requests/count`).
 *
 * Unlike [getWireMockRequestCount], which only sees the path, this asserts on a query parameter —
 * e.g. that a pagination request carried a specific cursor.
 *
 * @param method HTTP method to match (e.g. "GET")
 * @param urlPath Exact request path, without the query string (e.g. "/bff-v2/v1/customer/transactions")
 * @param queryParam Query parameter name to match (e.g. "cursor")
 * @param queryValue Exact expected value of [queryParam]
 * @param baseUrl WireMock base URL (defaults to local override if set, otherwise remote)
 * @return number of matching requests, or 0 if the journal could not be queried
 */
fun getWireMockRequestCountByQueryParam(
    method: String,
    urlPath: String,
    queryParam: String,
    queryValue: String,
    baseUrl: String = getWireMockBaseUrl(),
): Int {
    val client = OkHttpClient()
    // A queryParameters matcher instead of a urlPattern regex: no backslash escaping to get wrong.
    val json = JSONObject()
        .put("method", method)
        .put("urlPath", urlPath)
        .put("queryParameters", JSONObject().put(queryParam, JSONObject().put("equalTo", queryValue)))
        .toString()
    val mediaType = "application/json".toMediaType()

    val request = Request.Builder()
        .url("$baseUrl/__admin/requests/count")
        .post(json.toRequestBody(mediaType))
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            TangemLogger.d("WireMock request count ($queryParam=$queryValue): ${response.code} - $body")
            if (response.isSuccessful) JSONObject(body).getInt("count") else 0
        }
    } catch (e: IOException) {
        TangemLogger.e("WireMock request count (by query param) error", e)
        0
    } catch (e: JSONException) {
        TangemLogger.e("WireMock request count (by query param): unexpected response body", e)
        0
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
 * Method to reset a specific WireMock scenario to its initial state.
 *
 * Resets via an empty-body `PUT` rather than setting the state to "Started": `possibleStates` holds only
 * the states a scenario's mappings name, so for the ones that never name "Started" (`tangem_pay_balance_update`,
 * `kaspa_utxo`, …) an explicit state change is rejected with HTTP 422 and the scenario stays dirty for the
 * next test on this WireMock instance.
 *
 * @param scenarioName Name of the scenario to reset
 * @param baseUrl WireMock base URL (defaults to local override if set, otherwise remote)
 * @return true if reset was successful, false otherwise
 */
fun resetWireMockScenarioState(scenarioName: String, baseUrl: String = getWireMockBaseUrl()): Boolean {
    TangemLogger.i("=== WireMock Scenario Reset ===")
    TangemLogger.i("Resetting scenario '$scenarioName' to its initial state")

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$baseUrl/__admin/scenarios/$scenarioName/state")
        .put("".toRequestBody())
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            TangemLogger.d("WireMock scenario reset response: ${response.code} - ${response.message}")
            if (!response.isSuccessful) {
                TangemLogger.e("Failed to reset scenario '$scenarioName': ${response.code}")
            }
            response.isSuccessful
        }
    } catch (e: IOException) {
        TangemLogger.e("WireMock scenario reset error", e)
        false
    }
}