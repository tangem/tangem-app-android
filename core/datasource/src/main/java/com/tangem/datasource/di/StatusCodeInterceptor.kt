package com.tangem.datasource.di

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber

class StatusCodeInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())

        if (shouldInterceptResponse(originalResponse)) {
            Timber.e("StatusCodeInterceptor INTERCEPTED%s", originalResponse.request.url.toString())

            val body = getBody().toResponseBody("application/json".toMediaTypeOrNull())
            val code = getCode()

            return originalResponse.newBuilder()
                .code(code)
                .body(body)
                .build()
        }

        return originalResponse
    }

    private fun shouldInterceptResponse(response: Response): Boolean {
        return response.request.url.toString().contains("exchange-quote")
        // && response.request.url.toString().contains("changenow")
    }

    private fun getCode(): Int {
        return CODE_400
    }

    private fun getBody(): String {
        return "\"error\": {\n" +
            "        \"code\": 2290,\n" +
            "        \"description\": \"Core: receivedDecimals is not equal to expressDecimals\",\n" +
            "        \"message\": \"Not valid\",\n" +
            "        \"receivedToDecimals\": 5,\n" +
            "        \"expressToDecimals\": 5\n" +
            "    }"
    }

    companion object {
        private const val CODE_400 = 400
    }
}
