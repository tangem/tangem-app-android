package com.tangem.domain.transaction.error

sealed class IncompleteTransactionError {
    data class DataError(val message: String?) : IncompleteTransactionError()
}