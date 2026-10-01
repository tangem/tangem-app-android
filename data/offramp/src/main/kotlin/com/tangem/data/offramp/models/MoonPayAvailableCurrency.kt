package com.tangem.data.offramp.models

internal data class MoonPayAvailableCurrency(
    val currencyCode: String,
    val networkCode: String,
    val contractAddress: String?,
)