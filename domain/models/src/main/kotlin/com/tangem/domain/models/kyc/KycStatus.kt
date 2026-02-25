package com.tangem.domain.models.kyc

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val APPROVED_KYC_STATUS = "approved"
private const val IN_PROGRESS_KYC_STATUS = "in_progress"
private const val DECLINED_KYC_STATUS = "declined"

@JsonClass(generateAdapter = false)
enum class KycStatus {
    /** Initial state */
    @Json(name = "init")
    INIT,

    /** Performing the check */
    @Json(name = "in_progress")
    PENDING,

    /** SumSub approved */
    @Json(name = "approved")
    APPROVED,

    /** The check failed, documents rejected */
    @Json(name = "declined")
    REJECTED,

    ;

    companion object {

        fun fromString(status: String?, default: KycStatus = INIT): KycStatus {
            return when (status?.lowercase()) {
                IN_PROGRESS_KYC_STATUS -> PENDING
                DECLINED_KYC_STATUS -> REJECTED
                APPROVED_KYC_STATUS -> APPROVED
                else -> default
            }
        }
    }
}