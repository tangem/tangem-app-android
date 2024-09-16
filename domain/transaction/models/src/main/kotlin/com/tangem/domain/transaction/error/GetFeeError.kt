package com.tangem.domain.transaction.error

sealed class GetFeeError {
    data class DataError(val cause: Throwable?) : GetFeeError()
    data object UnknownError : GetFeeError()

    sealed class BlockchainErrors : GetFeeError() {
        data object TronActivationError : BlockchainErrors()
    }
}
