package com.tangem.common.utils

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
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

    val cookies = "CF_AppSession=ncc11a9cf4a877b6f; CF_Authorization=eyJhbGciOiJSUzI1NiIsImtpZCI6IjA3ZTBkYWRjMmU0ZWE5OTcxYThiYThkNmU0OTQxNzVhOTg3Njg1ZjhlOWQ1OTZlOGFhYzMxNmE1YmY2MGE1ZWEifQ.eyJhdWQiOlsiNWQ0YjA0YTM2YmRhMTI5MDE4NWJkY2RmZmI4ZTAyYmFmYTZmMzE3ODFjMWExMDE0MmM1ZWQ3M2U3N2NiYjU2ZSJdLCJlbWFpbCI6ImRwb2RveW5pa292QHRhbmdlbS5jb20iLCJleHAiOjE3NTU5NTU2MjIsImlhdCI6MTc1NTg2OTIyMiwibmJmIjoxNzU1ODY5MjIyLCJpc3MiOiJodHRwczovL3RhbmdlbS5jbG91ZGZsYXJlYWNjZXNzLmNvbSIsInR5cGUiOiJhcHAiLCJpZGVudGl0eV9ub25jZSI6IlpFYVpCTTlRbUtWYXNBUVIiLCJzdWIiOiIwMzcyYjhlYi01ZTJlLTU1MTUtOGUzYi0zZTY4NTA2NjhmN2MiLCJjb3VudHJ5IjoiREUifQ.emrjJMjCaoh9j7vt5KrAU7nSzTK4wT5-tlId8evB4pVNUtsoG83bHJRz4Oj7Ts8Th3IEI5Kcxf4eXvXl1NyrRo07pDA0tWR-DmtvniImy4YXE522-LSyd44XGUQMWVrmOPRCPpGJF9xvIkAxOGP-JeUPgKuKAYZsmsQAKYTL-KBdJ2Qw4BHWU3WnEPm9o8y4tScun19R_6gGV0rCkksHWaBRh68NGYutTtzclrXvJkbwpAUqCpWK0qY0x22S5GnjKC0fhWEQ11Hz5d1AN6f-lX7lkAMmzyLfBYUkgv1I29nwJHwCqbfEP8rcNgmdhZt4DQ2bTpycCCI63Vifl-pmhA"

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$baseUrl/health")
        .addHeader("Cookie", cookies) // Добавляем куки для авторизации
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

fun openAppWithDeeplink() {
    val deepLinkScheme = "tangem://wc?uri="
    val deepLinkUri = deepLinkScheme + getWcUri()
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
}