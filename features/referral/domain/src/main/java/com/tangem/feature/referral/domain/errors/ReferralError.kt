package com.tangem.feature.referral.domain.errors

sealed class ReferralError : Exception() {
    data object UserCancelledException : ReferralError()
    data object SdkError : ReferralError()

    data class DataError(val throwable: Throwable) : ReferralError()
}