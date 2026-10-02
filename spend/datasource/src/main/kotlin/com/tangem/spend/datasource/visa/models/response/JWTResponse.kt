package com.tangem.spend.datasource.visa.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JWTResponse(
    @SerialName("result") val result: Result,
) {
    @Serializable
    data class Result(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Int,
        @SerialName("refresh_expires_in") val refreshExpiresIn: Int,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("token_type") val tokenType: String,
        @SerialName("not-before-policy") val notBeforePolicy: Int,
        @SerialName("session_state") val sessionState: String,
        @SerialName("scope") val scope: String,
    )
}