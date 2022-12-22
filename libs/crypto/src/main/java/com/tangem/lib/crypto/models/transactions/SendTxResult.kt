package com.tangem.lib.crypto.models.transactions

sealed interface SendTxResult {

    object Success : SendTxResult
    object UserCancelledError : SendTxResult
    data class TangemSdkError(val code: Int, val cause: Throwable?) : SendTxResult
    data class BlockchainSdkError(val code: Int, val cause: Throwable?) : SendTxResult
    data class UnknownError(val ex: Exception? = null) : SendTxResult
}
