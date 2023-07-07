package com.tangem.feature.learn2earn.domain.api

import com.tangem.feature.learn2earn.analytics.Learn2earnEvents

/**
 * Handler that helps determine a result of the Learn2earnWebViewActivity webView actions.
 *
[REDACTED_AUTHOR]
 */
interface WebViewResultHandler {
    fun handleResult(result: WebViewResult)
}

sealed class WebViewResult {
    object Empty : WebViewResult()
    data class PromoCode(val promoCode: String) : WebViewResult()
    object ReadyForAward : WebViewResult()
    data class AnalyticsEvent(val event: Learn2earnEvents) : WebViewResult()
}