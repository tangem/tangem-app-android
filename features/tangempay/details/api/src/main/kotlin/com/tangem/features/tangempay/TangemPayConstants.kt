package com.tangem.features.tangempay

import androidx.annotation.VisibleForTesting
import com.tangem.utils.withCacheBypass

object TangemPayConstants {

    @VisibleForTesting
    const val PRIVACY_POLICY_URL = "https://tangem.com/docs/en/privacy-policy.pdf"

    @VisibleForTesting
    const val TERMS_AND_LIMITS_URL = "https://tangem.com/docs/en/tangem-visa-tariffs.pdf"

    private const val PAERA_TERMS_AND_FEES_URL = "https://paera.com/tangempay"

    fun tangemPayLink(): String = PAERA_TERMS_AND_FEES_URL.withCacheBypass()

    fun privacyPolicyLink(): String = PRIVACY_POLICY_URL.withCacheBypass()

    fun termsAndLimitsLink(): String = TERMS_AND_LIMITS_URL.withCacheBypass()
}