package com.tangem.domain.pay

sealed class WithdrawalSignatureResult {

    data class Success(val signature: String) : WithdrawalSignatureResult()

    data object Cancelled : WithdrawalSignatureResult()
}