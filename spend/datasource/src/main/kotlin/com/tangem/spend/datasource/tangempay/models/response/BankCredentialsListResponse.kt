package com.tangem.spend.datasource.tangempay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BankCredentialsListResponse(
    @SerialName("result") val result: List<BankCredentialsResponse.Result>?,
)