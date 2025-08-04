package com.tangem.domain.walletconnect.model

import com.tangem.domain.transaction.error.SendTransactionError

sealed class WcRequestError {

    data class WrappedSendError(
        val sendTransactionError: SendTransactionError,
    ) : WcRequestError()

    data class WcRespondError(
        val code: Int,
        val message: String,
    ) : WcRequestError()

    data class UnknownError(val ex: Throwable? = null) : WcRequestError()

    companion object {

        fun WcRequestError.message(): String? = when (this) {
            is UnknownError -> ex?.message
            is WcRespondError -> message
            is WrappedSendError -> sendTransactionError.message()
            is HandleMethodError -> message
        }

        fun WcRequestError.code(): String? = when (this) {
            is HandleMethodError,
            is UnknownError,
            -> null
            is WcRespondError -> this.code.toString()
            is WrappedSendError -> sendTransactionError.code()
        }

        private fun SendTransactionError.code(): String? = when (this) {
            is SendTransactionError.BlockchainSdkError -> code.toString()
            is SendTransactionError.NetworkError -> code
            is SendTransactionError.TangemSdkError -> code.toString()
            is SendTransactionError.DataError,
            SendTransactionError.DemoCardError,
            is SendTransactionError.UnknownError,
            SendTransactionError.UserCancelledError,
            is SendTransactionError.CreateAccountUnderfunded,
            -> null
        }

        private fun SendTransactionError.message(): String? = when (this) {
            is SendTransactionError.BlockchainSdkError -> message
            is SendTransactionError.NetworkError -> message
            is SendTransactionError.DataError -> message
            is SendTransactionError.UnknownError -> ex?.message
            is SendTransactionError.TangemSdkError,
            SendTransactionError.DemoCardError,
            SendTransactionError.UserCancelledError,
            is SendTransactionError.CreateAccountUnderfunded,
            -> null
        }
    }
}

sealed class HandleMethodError(
    open val message: String,
) : WcRequestError() {

    data class Unsupported(
        val method: WcMethod.Unsupported,
    ) : HandleMethodError(message = "Unsupported method ${method.request.request.method}")

    data object UnknownSession : HandleMethodError(message = "WalletConnect session was disconnected")

    data class UnknownError(override val message: String) : HandleMethodError(message)
}