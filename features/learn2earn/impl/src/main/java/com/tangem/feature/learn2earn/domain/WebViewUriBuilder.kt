package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.common.Provider
import com.tangem.data.common.locale.LocaleProvider
import com.tangem.feature.learn2earn.impl.BuildConfig

/**
[REDACTED_AUTHOR]
 */
internal class WebViewUriBuilder(
    private val authCredentialsProvider: Provider<String?>,
    private val localeProvider: LocaleProvider,
    private val promoCodeProvider: Provider<String?>,
) {

    fun buildUriForNewUser(learningIsFinished: Boolean): Uri {
        val builder = makeWebViewUriBuilder(learningIsFinished)
            .appendQueryParameter("type", QUERY_NEW_CARD)

        promoCodeProvider.invoke()?.let {
            builder.appendQueryParameter("code", it)
        }

        return builder.build()
    }

    fun buildUriForOldUser(learningIsFinished: Boolean): Uri {
        val builder = makeWebViewUriBuilder(learningIsFinished)
            .appendQueryParameter("type", QUERY_EXISTING_CARD)

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

    private fun makeWebViewUriBuilder(learningIsFinished: Boolean): Uri.Builder = Uri.Builder().apply {
        scheme(SCHEME)
        if (BuildConfig.DEBUG) {
            authority(DEV_BASE_URL)
        } else {
            authority(BASE_URL)
        }
        appendPath(localeProvider.getWebUriLocaleLanguage())
        appendPath(PATH_PROMOTION)
        appendQueryParameter(QUERY_FINISHED, learningIsFinished.toString())
    }

    private companion object {
        const val SCHEME = "https"

        const val BASE_URL = "tangem.com"
        const val PATH_PROMOTION = "promotion"

        const val QUERY_NEW_CARD = "new-card"
        const val QUERY_EXISTING_CARD = "existing-card"
        const val QUERY_FINISHED = "finished"

        const val DEV_BASE_URL = "devweb.tangem.com"
    }
}