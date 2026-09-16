package com.tangem.domain.pay.model

import com.tangem.domain.models.pay.TangemPayProduct

data class TangemPayWaitlistEntry(
    val email: String,
    val product: TangemPayProduct,
    val countryCode: String,
    val regionCode: String?,
    val isMarketingConsentGiven: Boolean,
)