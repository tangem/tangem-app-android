package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeeplinkValidityRequest(
    @SerialName("link") val link: String,
)