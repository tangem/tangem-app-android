package com.tangem.grow.datasource.earnvault.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** [code] is absent on the service-wide `400 Validation failed` answer */
@Serializable
data class EarnVaultErrorResponse(
    @SerialName("code")
    val code: EarnVaultErrorCodeDto = EarnVaultErrorCodeDto.UNKNOWN,
    @SerialName("message")
    val message: String? = null,
    @SerialName("details")
    val details: Details? = null,
) {

    /** [lockedUntil] comes with `UNITS_LOCKED` when the deposit was made in the app */
    @Serializable
    data class Details(
        @SerialName("lockedUntil")
        val lockedUntil: String? = null,
    )
}

@Serializable
enum class EarnVaultErrorCodeDto {
    VAULT_UNAVAILABLE,
    VAULT_MARKET_NOT_FOUND,
    INSUFFICIENT_BALANCE,
    UNITS_LOCKED,
    MODE_UNSUPPORTED,
    RECEIVER_MISMATCH,
    UNKNOWN_TRANSACTION,
    INVALID_TRANSACTION,
    GENERIC,

    /** Not a wire value: absent or a code this build does not know, coerced by `NetworkJsonFactory` */
    UNKNOWN,
}