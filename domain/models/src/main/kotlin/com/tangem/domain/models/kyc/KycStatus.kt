package com.tangem.domain.models.kyc

private const val APPROVED_KYC_STATUS = "approved"
private const val IN_PROGRESS_KYC_STATUS = "in_progress"
private const val DECLINED_KYC_STATUS = "declined"

enum class KycStatus {
    /** Initial state */
    INIT,

    /** Performing the check */
    PENDING,

    /** SumSub approved */
    APPROVED,

    /** The check failed, documents rejected */
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