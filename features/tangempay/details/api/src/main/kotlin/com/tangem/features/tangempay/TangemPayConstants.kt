package com.tangem.features.tangempay

import androidx.annotation.VisibleForTesting

object TangemPayConstants {

    @VisibleForTesting
    const val PRIVACY_POLICY_URL = "https://tangem.com/docs/en/privacy-policy.pdf"

    @VisibleForTesting
    const val TERMS_AND_LIMITS_URL = "https://tangem.com/docs/en/tangem-visa-tariffs.pdf"

    fun privacyPolicyLink(): String = "$PRIVACY_POLICY_URL?t=${System.currentTimeMillis()}"

    fun termsAndLimitsLink(): String = "$TERMS_AND_LIMITS_URL?t=${System.currentTimeMillis()}"
}