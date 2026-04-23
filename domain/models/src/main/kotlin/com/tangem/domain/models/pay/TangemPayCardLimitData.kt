package com.tangem.domain.models.pay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayCardLimitData(
    @SerialName("actual_card_limit") val actualCardLimit: TangemPayCardLimit?,
    @SerialName("admin_card_limit") val adminCardLimit: TangemPayCardLimit?,
)