package com.tangem.domain.models.kyc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val UNKNOWN_KYC_STATUS = "unknown"
private const val REQUIRED_KYC_STATUS = "required"
private const val INIT_KYC_STATUS = "init"
private const val APPROVED_KYC_STATUS = "approved"
private const val IN_PROGRESS_KYC_STATUS = "in_progress"
private const val DECLINED_KYC_STATUS = "declined"

@Serializable
enum class KycStatus {
    @SerialName(UNKNOWN_KYC_STATUS)
    UNKNOWN,

    @SerialName(REQUIRED_KYC_STATUS)
    REQUIRED,

    /** Initial state */
    @SerialName(INIT_KYC_STATUS)
    INIT,

    /** Performing the check */
    @SerialName(IN_PROGRESS_KYC_STATUS)
    PENDING,

    /** SumSub approved */
    @SerialName(APPROVED_KYC_STATUS)
    APPROVED,

    /** The check failed, documents rejected */
    @SerialName(DECLINED_KYC_STATUS)
    REJECTED,

    ;

    companion object {

        fun fromString(status: String?, default: KycStatus = UNKNOWN): KycStatus {
            return when (status?.lowercase()) {
                REQUIRED_KYC_STATUS -> REQUIRED
                INIT_KYC_STATUS -> INIT
                IN_PROGRESS_KYC_STATUS -> PENDING
                DECLINED_KYC_STATUS -> REJECTED
                APPROVED_KYC_STATUS -> APPROVED
                else -> default
            }
        }
    }
}