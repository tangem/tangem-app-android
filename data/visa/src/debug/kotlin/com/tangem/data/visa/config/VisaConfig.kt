package com.tangem.data.visa.config

import com.squareup.moshi.Json

internal data class VisaConfig(
    @Json(name = "testnet")
    val testnet: Addresses,
    @Json(name = "mainnet")
    val mainnet: Addresses,
    @Json(name = "txHistoryAPIAdditionalHeaders")
    val header: Header,
) {

    data class Addresses(
        @Json(name = "paymentAccountRegistry")
        val paymentAccountRegistry: String,
        @Json(name = "bridgeProcessor")
        val bridgeProcessor: String,
    )

    data class Header(
        @Json(name = "x-asn")
        val xAsn: String,
    )
}
