package com.tangem.domain.visa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisaDataForApprove(
    @SerialName("targetAddress") val targetAddress: String,
    @SerialName("approveHash") val approveHash: String,
)