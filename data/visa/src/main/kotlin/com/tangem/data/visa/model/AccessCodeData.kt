package com.tangem.data.visa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AccessCodeData(
    @SerialName("pid") val productInstanceId: String,
    @SerialName("sub") val customerId: String,
)