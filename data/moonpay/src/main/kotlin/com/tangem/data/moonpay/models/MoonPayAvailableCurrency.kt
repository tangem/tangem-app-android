package com.tangem.data.moonpay.models

internal data class MoonPayAvailableCurrency(
    val currencyCode: String,
    val networkCode: String,
    val contractAddress: String?,
)