package com.tangem.feature.learn2earn.domain.models

import android.net.Uri

/**
[REDACTED_AUTHOR]
 */
interface WebViewHelper {
    fun handleWebViewRedirect(uri: Uri): RedirectConsequences
    fun getWebViewHeaders(): Map<String, String>
}

enum class RedirectConsequences {
    NOTHING,
    PROCEED,
    FINISH_SESSION,
}