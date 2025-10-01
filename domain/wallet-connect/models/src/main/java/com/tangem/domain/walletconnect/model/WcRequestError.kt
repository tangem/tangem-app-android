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

        fun WcRequestError.message(): String = when (this) {
            is UnknownError -> ex?.message ?: "UnknownError"
            is WcRespondError -> message
            is WrappedSendError -> sendTransactionError.message()
            is HandleMethodError -> message
        }

        fun WcRequestError.code(): String = when (this) {
            is HandleMethodError -> "HandleMethodError"
            is UnknownError -> "UnknownError"
            is WcRespondError -> this.code.toString()
            is WrappedSendError -> sendTransactionError.code()
        }

        private fun SendTransactionError.code(): String = when (this) {
            is SendTransactionError.BlockchainSdkError -> code.toString()
            is SendTransactionError.NetworkError -> code ?: "NetworkError"
            is SendTransactionError.TangemSdkError -> code.toString()
            is SendTransactionError.DataError -> "DataError"
            SendTransactionError.DemoCardError -> "DemoCardError"
            is SendTransactionError.UnknownError -> "UnknownError"
            SendTransactionError.UserCancelledError -> "UserCancelledError"
            is SendTransactionError.CreateAccountUnderfunded -> "CreateAccountUnderfunded"
        }

        private fun SendTransactionError.message(): String = when (this) {
            is SendTransactionError.BlockchainSdkError -> message ?: "BlockchainSdkError"
            is SendTransactionError.NetworkError -> message ?: "NetworkError"
            is SendTransactionError.DataError -> message ?: "DataError"
            is SendTransactionError.UnknownError -> ex?.message ?: "UnknownError"
            is SendTransactionError.TangemSdkError -> "TangemSdkError code: ${this.code}"
            SendTransactionError.DemoCardError -> "DemoCardError"
            SendTransactionError.UserCancelledError -> "UserCancelledError"
            is SendTransactionError.CreateAccountUnderfunded,
            -> "CreateAccountUnderfunded"
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
    data class TangemUnsupportedNetwork(val unsupportedNetwork: String) :
        HandleMethodError("TangemUnsupportedNetwork $unsupportedNetwork")

    data class NotAddedNetwork(val networkName: String) : HandleMethodError("NotAddedNetwork $networkName")
    data class RequiredNetwork(val networkName: String) : HandleMethodError("RequiredNetwork $networkName")
}