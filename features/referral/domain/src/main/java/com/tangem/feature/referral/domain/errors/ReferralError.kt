package com.tangem.feature.referral.domain.errors

sealed class ReferralError : Exception() {
    class UserCancelledException : ReferralError()
    class SdkError : ReferralError()

    class DataError(val throwable: Throwable) : ReferralError()
}