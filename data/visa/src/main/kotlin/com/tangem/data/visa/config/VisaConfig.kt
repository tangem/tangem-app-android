package com.tangem.data.visa.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VisaConfig(
    @SerialName("testnet")
    val testnet: Addresses,
    @SerialName("mainnet")
    val mainnet: Addresses,
    @SerialName("txHistoryAPIAdditionalHeaders")
    val header: Header,
    @SerialName("rsaPublicKey")
    val rsaPublicKey: RsaPublicKey,
    @SerialName("rainRSAPublicKey")
    val rainRSAPublicKey: RsaPublicKey,
) {

    @Serializable
    data class Addresses(
        @SerialName("paymentAccountRegistry")
        val paymentAccountRegistry: String,
        @SerialName("bridgeProcessor")
        val bridgeProcessor: String,
    )

    @Serializable
    data class Header(
        @SerialName("x-asn")
        val xAsn: String,
    )

    @Serializable
    data class RsaPublicKey(
        @SerialName("prod") val prod: String,
        @SerialName("dev") val dev: String,
    )
}