package com.tangem.data.tangempay.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TangemPayConfig(
    @SerialName("rainRSAPublicKey")
    val rainRSAPublicKey: RsaPublicKey,
) {

    @Serializable
    data class RsaPublicKey(
        @SerialName("prod") val prod: String,
        @SerialName("dev") val dev: String,
    )
}