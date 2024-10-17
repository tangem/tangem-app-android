package com.tangem.domain.onramp.model

data class OnrampCountry(
    val name: String,
    val code: String,
    val image: String,
    val alpha3: String,
    val continent: String,
    val defaultCurrency: OnrampCurrency,
    val onrampAvailable: Boolean,
)