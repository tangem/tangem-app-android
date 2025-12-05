package com.tangem.domain.pay.model

sealed class WithdrawalSignatureResult {

    data class Success(val signature: String) : WithdrawalSignatureResult()

    data object Cancelled : WithdrawalSignatureResult()
}