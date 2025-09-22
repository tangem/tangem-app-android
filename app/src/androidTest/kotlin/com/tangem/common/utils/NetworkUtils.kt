package com.tangem.common.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 *
 */
fun getWcUri(
    network: String = "ethereum",
    baseUrl: String = "[REDACTED_ENV_URL]"
): String? {
    Timber.i("Getting WC URI for network: $network")

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)    // Таймаут подключения
        .readTimeout(60, TimeUnit.SECONDS)       // Таймаут чтения ответа
        .writeTimeout(30, TimeUnit.SECONDS)      // Таймаут записи
        .callTimeout(90, TimeUnit.SECONDS)       // Общий таймаут запроса
        .build()

    val request = Request.Builder()
        .url("$baseUrl/wc_uri?network=$network")
        .get()
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            Timber.i("Response code: ${response.code}")

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Timber.i("Response body: $body")

                val jsonObject = JSONObject(body)

                if (jsonObject.getBoolean("success")) {
                    val wcUri = jsonObject.getString("wcUri")
                    Timber.i("Got WC URI successfully: $wcUri")

                    wcUri
                } else {
                    Timber.e("API returned error: ${jsonObject.optString("error", "Unknown")}")
                    null
                }
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                Timber.e("Request failed: ${response.code}, body: $errorBody")
                null
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error getting WC URI")
        null
    }
}

fun checkServiceHealth(
    baseUrl: String = "[REDACTED_ENV_URL]"
): String? {
    Timber.i("Checking service health")

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$baseUrl/health")
        .get()
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            Timber.i("Response code: ${response.code}")

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Timber.i("Response body: $body")

                if (body.isEmpty()) {
                    Timber.e("Response body is empty")
                    return null
                }

                val jsonObject = JSONObject(body)
                val status = jsonObject.optString("status", "")

                if (status.isNotEmpty()) {
                    Timber.i("Got status successfully: $status")
                    status
                } else {
                    Timber.e("Status field is missing or empty")
                    null
                }
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                Timber.e("Request failed: ${response.code}, body: $errorBody")
                null
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error checking health")
        null
    }
}