package com.tangem.features.tangempay

import androidx.annotation.VisibleForTesting

object TangemPayConstants {

    @VisibleForTesting
    const val TERMS_AND_LIMITS_URL = "https://tangem.com/docs/en/tangem-visa-tariffs.pdf"

    fun termsAndLimitsLink(): String = "$TERMS_AND_LIMITS_URL?t=${System.currentTimeMillis()}"
}