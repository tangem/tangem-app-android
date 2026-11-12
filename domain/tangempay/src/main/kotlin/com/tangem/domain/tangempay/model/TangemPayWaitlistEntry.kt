package com.tangem.domain.tangempay.model

data class TangemPayWaitlistEntry(
    val email: String,
    val countryCode: String,
    val regionCode: String?,
    val isMarketingConsentGiven: Boolean,
)