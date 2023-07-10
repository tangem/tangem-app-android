package com.tangem.feature.learn2earn.domain.api

import android.net.Uri

/**
 * Handler that helps determine what actions to take inside the WebView of the Learn2earnWebViewActivity after
 * a redirect has been processed by WebViewRedirectHandler
 *
* [REDACTED_AUTHOR]
 */
interface WebViewRedirectHandler {
    fun handleRedirect(uri: Uri): WebViewAction
}

enum class WebViewAction {
    NOTHING,
    PROCEED,
    FINISH_SESSION,
}
