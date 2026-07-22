package com.tangem.domain.transaction.error

sealed class GetFeeError {
    data class DataError(val cause: Throwable?) : GetFeeError()
    data object UnknownError : GetFeeError()

    sealed class BlockchainErrors : GetFeeError() {
        data object TronActivationError : BlockchainErrors()
        data object KaspaZeroUtxo : BlockchainErrors()
        data object SuiOneCoinRequired : BlockchainErrors()
        data object TooLargeSolanaTransactionError : BlockchainErrors()
    }

    /**
     * Gasless transaction related errors, model logic uses this errors types, don't remove or change them
     */
    sealed class GaslessError : GetFeeError() {
        data object NetworkIsNotSupported : GaslessError()
        data object NoSupportedTokensFound : GaslessError()
        data object NotEnoughFunds : GaslessError()
        data object ModuleUpdateUnavailable : GaslessError()
        data class DataError(val cause: Throwable?) : GaslessError()
    }

    /**
     * Error for gas estimation with state override for ethereum like networks.
     * Specifically overriding approval slot.
     */
    data class EstimateOverrideError(
        val blockchain: String,
        val tokenSymbol: String,
        val rpcProvider: String,
        val error: String,
    ) : GetFeeError()
}