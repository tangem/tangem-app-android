package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.feature.learn2earn.analytics.Learn2earnEvents
import com.tangem.feature.learn2earn.analytics.Learn2earnEvents.PromoScreen
import com.tangem.feature.learn2earn.domain.api.WebViewResult

/**
* [REDACTED_AUTHOR]
 */
internal class WebViewUriParser(
    private val programNameProvider: () -> String,
) {

    fun parse(uri: Uri): WebViewResult = when {
        isAnalyticsRedirect(uri) -> {
            val event = extractAnalyticsEvent(uri)
            if (event == null) {
                WebViewResult.Empty
            } else {
                WebViewResult.Learn2earnAnalyticsEvent(event)
            }
        }
        isSuccessNewUserRedirect(uri) -> {
            val promoCode = extractPromoCode(uri)
            if (promoCode == null) {
                WebViewResult.Empty
            } else {
                WebViewResult.NewUserLearningFinished(promoCode)
            }
        }
        isSuccessOldUserRedirect(uri) -> {
            WebViewResult.OldUserLearningFinished
        }
        isReadyForAwardRedirect(uri) -> {
            WebViewResult.ReadyForAward
        }
        else -> WebViewResult.Empty
    }

    private fun isReadyForAwardRedirect(uri: Uri): Boolean {
        return uri.lastPathSegment == PATH_READY_FOR_AWARD
    }

    private fun isSuccessNewUserRedirect(uri: Uri): Boolean {
        return uri.lastPathSegment == PATH_LEARNING_SUCCESS &&
            uri.queryParameterNames.contains(QUERY_PROMO_CODE)
    }

    private fun isSuccessOldUserRedirect(uri: Uri): Boolean {
        return uri.lastPathSegment == PATH_LEARNING_SUCCESS &&
            !uri.queryParameterNames.contains(QUERY_PROMO_CODE)
    }

    private fun extractPromoCode(uri: Uri): String? {
        return if (isSuccessNewUserRedirect(uri)) {
            uri.getQueryParameter(QUERY_PROMO_CODE)
        } else {
            null
        }
    }

    private fun isAnalyticsRedirect(uri: Uri): Boolean {
        return uri.lastPathSegment == PATH_ANALYTICS &&
            uri.queryParameterNames.contains(QUERY_ANALYTICS_EVENT)
    }

    private fun extractAnalyticsEvent(uri: Uri): Learn2earnEvents? {
        if (!isAnalyticsRedirect(uri)) return null

        val programName = uri.getQueryParameter(QUERY_PROGRAM_NAME) ?: return null
        if (programName != programNameProvider.invoke()) return null

        val event = uri.getQueryParameter(QUERY_ANALYTICS_EVENT) ?: return null

        val analyticsEvent = when (event) {
            EVENT_PROMO_BUY -> PromoScreen.ButtonBuy()
            else -> null
        }
        return analyticsEvent
    }

    private companion object {
        const val PATH_LEARNING_SUCCESS = "success"
        const val PATH_READY_FOR_AWARD = "ready-for-existing-card-award"
        const val PATH_ANALYTICS = "analytics"

        const val QUERY_PROMO_CODE = "code"
        const val QUERY_ANALYTICS_EVENT = "event"
        const val QUERY_PROGRAM_NAME = "programName"

        const val EVENT_PROMO_BUY = "promotion-buy"
    }
}
