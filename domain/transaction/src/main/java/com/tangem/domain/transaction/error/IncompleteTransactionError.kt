package com.tangem.domain.transaction.error

sealed class IncompleteTransactionError {
    data class SendError(val error: SendTransactionError) : IncompleteTransactionError()
    data class DataError(val message: String?) : IncompleteTransactionError()
}