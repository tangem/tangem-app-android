package com.tangem.network.common

import okhttp3.Interceptor
import okhttp3.Response

/**
[REDACTED_AUTHOR]
 */
open class AddHeaderInterceptor(
    private val headers: Map<String, String>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            headers.forEach {
                addHeader(it.key, it.value)
            }
        }.build()

        return chain.proceed(request)
    }
}

class CacheControlHttpInterceptor(maxAgeSeconds: Int) : AddHeaderInterceptor(mapOf(
    "Cache-Control" to "max-age=$maxAgeSeconds",
))

class CardPublicKeyHttpInterceptor(cardPublicKeyHex: String) : AddHeaderInterceptor(mapOf(
    "card_public_key" to cardPublicKeyHex,
))