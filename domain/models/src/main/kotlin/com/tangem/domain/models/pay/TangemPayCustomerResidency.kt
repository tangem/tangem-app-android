package com.tangem.domain.models.pay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayCustomerResidency(
    @SerialName("country_code") val countryCode: String,
    @SerialName("region_code") val regionCode: String?,
)