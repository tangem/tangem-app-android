package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.feature.learn2earn.impl.BuildConfig

/**
[REDACTED_AUTHOR]
 */
internal class WebViewUriBuilder(
    private val authCredentials: String?,
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

    fun buildUriForOlgUser(): Uri {
        val builder = makeWebViewUriBuilder()
            .appendQueryParameter("type", QUERY_EXISTED_CARD)

        return builder.build()
    }

    fun getBasicAuthHeaders(): Map<String, String> = if (authCredentials == null) {
        mapOf()
    } else {
        mapOf("Authorization" to "Basic $authCredentials")
    }

    fun isPromoCodeRedirect(uri: Uri): Boolean {
        return uri.toString().contains(SUFFIX_CODE_CREATED)
    }

    fun isReadyForExistedCardAwardRedirect(uri: Uri): Boolean {
        return uri.toString().endsWith(SUFFIX_READY_FOR_AWARD)
    }

    fun extractPromoCode(uri: Uri): String? {
        val url = uri.toString()

        return if (isPromoCodeRedirect(uri)) {
            val replaceString = makeWebViewUriBuilder().build().toString() + SUFFIX_CODE_CREATED
            val code = url.replace(replaceString, "")
            code
        } else {
            null
        }
    }

    private fun makeWebViewUriBuilder(): Uri.Builder {
        val builder = Uri.Builder()
        builder.scheme(SCHEME)

        if (BuildConfig.DEBUG) {
            builder.authority(DEV_BASE_URL)
            builder.appendPath(userCountryCodeProvider.invoke())
            builder.appendPath(DEV_PATH_PROMOTION)
        } else {
            builder.authority(BASE_URL)
            builder.appendPath(userCountryCodeProvider.invoke())
            builder.appendPath(PATH_PROMOTION)
        }

        return builder
    }

    private companion object {
        const val SCHEME = "https"

        const val BASE_URL = "tangem.com"
        const val PATH_PROMOTION = "promotion"

        const val QUERY_NEW_CARD = "new-card"
        const val QUERY_EXISTED_CARD = "existed-card"

        const val DEV_BASE_URL = "devweb.tangem.com"
        const val DEV_PATH_PROMOTION = "promotion-test"

        const val SUFFIX_CODE_CREATED = "/code-created?code="
        const val SUFFIX_READY_FOR_AWARD = "/ready-for-existed-card-award"
    }
}