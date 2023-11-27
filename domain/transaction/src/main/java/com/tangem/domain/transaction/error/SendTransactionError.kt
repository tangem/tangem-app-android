package com.tangem.domain.transaction.error

sealed class SendTransactionError {

    object DemoCardError : SendTransactionError()

    data class DataError(val message: String?) : SendTransactionError()

    data class NetworkError(val message: String?) : SendTransactionError()
}