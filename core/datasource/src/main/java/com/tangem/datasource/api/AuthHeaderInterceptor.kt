package com.tangem.datasource.api

import com.tangem.lib.auth.AuthProvider
import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(
    private val authProvider: AuthProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            addHeader(
                HEADER_CARD_KEY, authProvider.getAuthToken(),
            )
        }.build()

        return chain.proceed(request)
    }

    companion object {
        const val HEADER_CARD_KEY = "card_public_key"
    }
}