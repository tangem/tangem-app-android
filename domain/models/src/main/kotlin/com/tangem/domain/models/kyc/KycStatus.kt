package com.tangem.domain.models.kyc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val INIT_KYC_STATUS = "init"
private const val APPROVED_KYC_STATUS = "approved"
private const val IN_PROGRESS_KYC_STATUS = "in_progress"
private const val DECLINED_KYC_STATUS = "declined"

@Serializable
enum class KycStatus {
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