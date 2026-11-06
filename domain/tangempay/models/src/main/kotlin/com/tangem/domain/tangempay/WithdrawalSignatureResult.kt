package com.tangem.domain.tangempay

sealed class WithdrawalSignatureResult {

    data class Success(val signature: String) : WithdrawalSignatureResult()

    data object Cancelled : WithdrawalSignatureResult()
}