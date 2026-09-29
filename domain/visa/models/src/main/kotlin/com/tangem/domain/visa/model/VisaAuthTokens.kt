package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaAuthTokens(
    val accessToken: String,
    val refreshToken: RefreshToken,
) {

    @Serializable
    data class RefreshToken(
        val value: String,
        val authType: Type,
    ) {

        enum class Type {
            CardId, CardWallet,
        }
    }
}

fun VisaAuthTokens.getAuthHeader(): String {
    return "Bearer $accessToken"
}