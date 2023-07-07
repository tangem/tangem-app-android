package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.feature.learn2earn.impl.BuildConfig

/**
[REDACTED_AUTHOR]
 */
internal class WebViewUriBuilder(
    private val authCredentialsProvider: () -> String?,
    private val userCountryCodeProvider: () -> String,
    private val promoCodeProvider: () -> String?,
) {

    fun buildUriForNewUser(): Uri {
        val builder = makeWebViewUriBuilder()
            .appendQueryParameter("type", QUERY_NEW_CARD)

        promoCodeProvider.invoke()?.let {
            builder.appendQueryParameter("code", it)
        }

        return builder.build()
    }

    fun buildUriForOldUser(): Uri {
        val builder = makeWebViewUriBuilder()
            .appendQueryParameter("type", QUERY_EXISTED_CARD)

        return builder.build()
    }

    fun getBasicAuthHeaders(): Map<String, String> {
        val credentials = authCredentialsProvider.invoke()
        return if (credentials == null) {
            mapOf()
        } else {
            mapOf("Authorization" to "Basic $credentials")
        }
    }

    private fun makeWebViewUriBuilder(): Uri.Builder = Uri.Builder().apply {
        scheme(SCHEME)
        if (BuildConfig.DEBUG) {
            authority(DEV_BASE_URL)
        } else {
            authority(BASE_URL)
        }
        appendPath(userCountryCodeProvider.invoke())
        appendPath(PATH_PROMOTION)
    }

    private companion object {
        const val SCHEME = "https"

        const val BASE_URL = "tangem.com"
        const val PATH_PROMOTION = "promotion"

        const val QUERY_NEW_CARD = "new-card"
        const val QUERY_EXISTED_CARD = "existed-card"

        const val DEV_BASE_URL = "devweb.tangem.com"
    }
}