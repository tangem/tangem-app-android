package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetTangemPayEnabledRequest(
    @SerialName("is_tangem_pay_enabled") val isTangemPayEnabled: Boolean,
)