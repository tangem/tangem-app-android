package com.tangem.domain.transaction.error

sealed class SendTransactionError {

    data object DemoCardError : SendTransactionError()

    data class DataError(val message: String?) : SendTransactionError()

    data class NetworkError(val message: String?, val code: String?) : SendTransactionError()

    data class BlockchainSdkError(val code: Int, val message: String?) : SendTransactionError()

    data object UserCancelledError : SendTransactionError()

    data class CreateAccountUnderfunded(val amount: String) : SendTransactionError()

    data class TangemSdkError(val code: Int, val messageRes: Int, val args: List<Any>) : SendTransactionError()

    data class UnknownError(val ex: Exception? = null) : SendTransactionError()

    companion object {
        const val USER_CANCELLED_ERROR_CODE = 50002
    }
}