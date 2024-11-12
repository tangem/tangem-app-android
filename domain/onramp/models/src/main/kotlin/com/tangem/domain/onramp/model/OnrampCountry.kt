package com.tangem.domain.onramp.model

import kotlinx.serialization.Serializable

@Serializable
data class OnrampCountry(
    val id: String,
    val name: String,
    val code: String,
    val image: String,
    val alpha3: String,
    val continent: String,
    val defaultCurrency: OnrampCurrency,
    val onrampAvailable: Boolean,
)