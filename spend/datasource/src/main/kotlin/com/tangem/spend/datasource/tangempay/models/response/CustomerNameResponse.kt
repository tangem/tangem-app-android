package com.tangem.spend.datasource.tangempay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerNameResponse(
    @SerialName("result") val result: Result?,
) {

    @Serializable
    data class Result(
        @SerialName("first_name") val firstName: String? = null,
        @SerialName("last_name") val lastName: String? = null,
    )
}