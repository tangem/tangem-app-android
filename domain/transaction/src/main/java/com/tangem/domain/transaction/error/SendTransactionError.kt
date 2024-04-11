package com.tangem.domain.transaction.error

import com.tangem.core.ui.extensions.TextReference

sealed class SendTransactionError {

    object DemoCardError : SendTransactionError()

    data class DataError(val message: String?) : SendTransactionError()

    data class NetworkError(val message: String?, val code: String?) : SendTransactionError()

    data class BlockchainSdkError(val code: Int, val message: String?) : SendTransactionError()

    object UserCancelledError : SendTransactionError()

    data class CreateAccountUnderfunded(val amount: String) : SendTransactionError()

    data class TangemSdkError(val code: Int, val messageReference: TextReference) : SendTransactionError()

    data class UnknownError(val ex: Exception? = null) : SendTransactionError()

    companion object {
        const val USER_CANCELLED_ERROR_CODE = 50002
    }
}
