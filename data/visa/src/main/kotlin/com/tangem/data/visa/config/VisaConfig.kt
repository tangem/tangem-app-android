package com.tangem.data.visa.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class VisaConfig(
    @Json(name = "testnet")
    val testnet: Addresses,
    @Json(name = "mainnet")
    val mainnet: Addresses,
    @Json(name = "txHistoryAPIAdditionalHeaders")
    val header: Header,
    @Json(name = "rsaPublicKey")
    val rsaPublicKey: RsaPublicKey,
) {

    @JsonClass(generateAdapter = true)
    data class Addresses(
        @Json(name = "paymentAccountRegistry")
        val paymentAccountRegistry: String,
        @Json(name = "bridgeProcessor")
        val bridgeProcessor: String,
    )

    @JsonClass(generateAdapter = true)
    data class Header(
        @Json(name = "x-asn")
        val xAsn: String,
    )

    @JsonClass(generateAdapter = true)
    data class RsaPublicKey(
        @Json(name = "prod") val prod: String,
        @Json(name = "dev") val dev: String,
    )
}