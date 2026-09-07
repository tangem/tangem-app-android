package com.tangem.features.tangempay.eligibility.country

internal data class CountryOfResidence(
    val code: String,
    val name: String,
    val flagUrl: String,
    val hasStates: Boolean,
)

internal data class StateOfResidence(
    val code: String,
    val name: String,
)