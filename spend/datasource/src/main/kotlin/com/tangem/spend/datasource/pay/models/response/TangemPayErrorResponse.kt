package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TangemPayErrorResponse(
    @SerialName("error") val error: Error,
) {

    @Serializable
    data class Error(
        @SerialName("code") val code: Int,
        @SerialName("name") val name: String?,
    )

    companion object {

        fun decodeOrNull(json: Json, body: String?): TangemPayErrorResponse? {
            if (body == null) return null

            return runCatching { json.decodeFromString(serializer(), body) }.getOrNull()
        }
    }
}