package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.feature.learn2earn.domain.models.CardType
import com.tangem.feature.learn2earn.impl.BuildConfig
import com.tangem.lib.auth.BasicAuthProvider

/**
[REDACTED_AUTHOR]
 */
class WebViewUriBuilder(
    private val basicAuthProvider: BasicAuthProvider,
    private val userCountryCodeProvider: () -> String,
    private val promoCodeProvider: () -> String?,
    private val promoNameProvider: () -> String,
) {

    fun buildUriForStories(type: CardType): Uri {
        val builder = makeWebViewUriBuilder()
            .appendQueryParameter("type", type.typeName)

        promoCodeProvider.invoke()?.let {
            builder.appendQueryParameter("code", it)
        }

        return builder.build()
    }

    fun buildUriForMainPage(walletId: String, cardId: String, cardPubKey: String): Uri {
        val builder = makeWebViewUriBuilder()
            .appendQueryParameter("type", CardType.EXISTED.typeName)
            .appendQueryParameter("cardPublicKey", cardPubKey)
            .appendQueryParameter("cardId", cardId)
            .appendQueryParameter("walletId", walletId)
            .appendQueryParameter("programName", promoNameProvider.invoke())

        promoCodeProvider.invoke()?.let {
            builder.appendQueryParameter("code", it)
        }

        return builder.build()
    }

    fun getBasicAuthHeaders(): Map<String, String> {
        return basicAuthProvider.getCredentials()
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
        builder.scheme("https")

        if (BuildConfig.DEBUG) {
            builder.authority(DEV_WEB_VIEW_BASE_URL)
            builder.appendPath(userCountryCodeProvider.invoke())
            builder.appendPath("promotion-test")
        } else {
            builder.authority(WEB_VIEW_BASE_URL)
            builder.appendPath(userCountryCodeProvider.invoke())
            builder.appendPath("promotion")
        }

        return builder
    }

    private companion object {
        const val WEB_VIEW_BASE_URL = "tangem.com"

        const val DEV_WEB_VIEW_BASE_URL = "devweb.tangem.com"
        const val DEV_WEB_VIEW_BASIC_AUTH = "Basic dGFuZ2VtOnRhbmdlbWRldjc="
        // TODO: 1inch: replace by invalid auth
        // const val DEV_WEB_VIEW_BASIC_AUTH = "Basic paste valid value"

        const val SUFFIX_CODE_CREATED = "/code-created?code="
        const val SUFFIX_READY_FOR_AWARD = "/ready-for-exsited-card-award"
    }
}