package com.tangem.domain.transaction.error

sealed class SendTransactionError {

    object DemoCardError : SendTransactionError()

    data class DataError(val message: String?) : SendTransactionError()

    data class NetworkError(val message: String?) : SendTransactionError()

    data class BlockchainSdkError(val code: Int, val cause: Throwable?) : SendTransactionError()
    object UserCancelledError : SendTransactionError()
    data class TangemSdkError(val code: Int, val cause: Throwable?) : SendTransactionError()
    data class UnknownError(val ex: Exception? = null) : SendTransactionError()

    companion object {
        const val USER_CANCELLED_ERROR_CODE = 50002
    }
}
