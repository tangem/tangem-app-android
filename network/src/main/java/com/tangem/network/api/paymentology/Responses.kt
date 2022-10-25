package com.tangem.network.api.paymentology

import com.squareup.moshi.Json
import com.tangem.common.extensions.calculateHashCode

/**
* [REDACTED_AUTHOR]
 */
data class RegistrationResponse(
    val results: List<Item> = listOf(),
    val success: Boolean,
    val error: String?,
    val errorCode: Int?,
) {

    data class Item(
        @Json(name = "CID")
        val cardId: String,
        val passed: Boolean?,
        val active: Boolean?,
        @Json(name = "pin_set")
        val pinSet: Boolean?,
        @Json(name = "blockchain_init")
        val blockchainInit: Boolean?,
        @Json(name = "kyc_passed")
        val kycPassed: Boolean?,
        @Json(name = "kyc_provider")
        val kycProvider: String?,
        @Json(name = "kyc_date")
        val kycDate: String?,
        @Json(name = "kyc_status")
        val kycStatus: KYCStatus?,
        @Json(name = "disabled_by_admin")
        val disabledByAdmin: Boolean?,
        val error: String?,
    )
}

enum class KYCStatus {
    NOT_STARTED,
    STARTED,
    WAITING_FOR_APPROVAL,
    CORRECTION_REQUESTED,
    REJECTED,
    APPROVED,
}

data class AttestationResponse(
    val challenge: ByteArray?,
    val success: Boolean,
    val error: String?,
    val errorCode: Int?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttestationResponse

        if (!challenge.contentEquals(other.challenge)) return false
        if (success != other.success) return false
        if (error != other.error) return false
        if (errorCode != other.errorCode) return false

        return true
    }

    override fun hashCode(): Int = calculateHashCode(
        challenge.contentHashCode(),
        success.hashCode(),
        error.hashCode(),
        errorCode.hashCode(),
    )
}

data class RegisterWalletResponse(
    val success: Boolean,
    val error: String?,
    val errorCode: Int?,
)
