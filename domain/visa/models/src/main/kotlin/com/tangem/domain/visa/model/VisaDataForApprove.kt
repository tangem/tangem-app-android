package com.tangem.domain.visa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisaDataForApprove(
    @SerialName("customerWalletCardId") val customerWalletCardId: String?,
    @SerialName("targetAddress") val targetAddress: String,
    @SerialName("approveHash") val approveHash: String,
)