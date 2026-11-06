package com.tangem.domain.pay.model

data class TangemPayWaitlistEntry(
    val email: String,
    val countryCode: String,
    val regionCode: String?,
    val isMarketingConsentGiven: Boolean,
)