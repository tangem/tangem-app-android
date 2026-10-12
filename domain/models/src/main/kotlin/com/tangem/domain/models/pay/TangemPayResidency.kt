package com.tangem.domain.models.pay

data class TangemPayCountry(
    val code: String,
    val name: String,
    val flagUrl: String?,
    val isSuggested: Boolean,
    val hasRegions: Boolean,
)

data class TangemPayRegion(
    val code: String,
    val name: String,
    val isSuggested: Boolean,
)

data class TangemPayEligibility(
    val isAvailable: Boolean,
    val country: String,
    val region: String?,
)