package com.tangem.domain.transaction.error

sealed class GetFeeError {
    data class DataError(val cause: Throwable?) : GetFeeError()
    data object UnknownError : GetFeeError()

    sealed class BlockchainErrors : GetFeeError() {
        data object TronActivationError : BlockchainErrors()
        data object KaspaZeroUtxo : BlockchainErrors()
        data object SuiOneCoinRequired : BlockchainErrors()
    }

    sealed class GaslessError : GetFeeError() {
        data object NetworkIsNotSupported : GaslessError()
        data object NoSupportedTokensFound : GaslessError()
        data object NotEnoughFunds : GaslessError()
        data class DataError(val cause: Throwable?) : GaslessError()
    }
}