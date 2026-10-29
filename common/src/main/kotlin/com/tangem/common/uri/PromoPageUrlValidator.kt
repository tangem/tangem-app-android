package com.tangem.common.uri

import android.net.Uri

object PromoPageUrlValidator {

    // Kept apart from ExternalUrlValidator: these pages run JavaScript inside the app
    private val allowedHosts: Set<String> = setOf(
        "tangem.com",
        "www.tangem.com",
        "buy.tangem.com",
        "app.tangem.com",
        "tangem.surveysparrow.com",
        "feedback.tangem.com",
        "promo.tangem.com",
    )

    fun isAllowed(uri: Uri): Boolean =
        uri.scheme == "https" && uri.userInfo == null && uri.host?.lowercase() in allowedHosts
}