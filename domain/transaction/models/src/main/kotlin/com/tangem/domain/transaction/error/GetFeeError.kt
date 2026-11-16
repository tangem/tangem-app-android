package com.tangem.domain.transaction.error

import java.math.BigDecimal

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

        /**
         * The Tron account is not activated yet and its balance is below [minimumAmount], the floor
         * the gasless service requires before it sponsors the activation.
         */
        data class TronActivationNotEnoughFunds(
            val minimumAmount: BigDecimal,
            val tokenSymbol: String,
        ) : GaslessError()

        /**
         * The gasless service rate limits the activation sponsorship: [isPerWallet] when this wallet used up
         * its own attempts, otherwise the service-wide limit is reached.
         */
        data class TronActivationRateLimited(val isPerWallet: Boolean) : GaslessError()

        /**
         * The Tron gasless service refused the quote: the compensation-token allowances together with the
         * compensation need [required] while the sender holds only [balance]. Amounts are in the compensation token.
         *
         * @property allowance the requested approval, or what the swap router and spender may already pull.
         * @property allowances the swap [allowance] by router and spender address.
         * @property reapproval set for a swap that becomes payable once the allowance is lowered by a new approval.
         */
        data class TronAllowanceTooHigh(
            val balance: BigDecimal,
            val required: BigDecimal,
            val allowance: BigDecimal,
            val allowances: Map<String, BigDecimal> = emptyMap(),
            val reapproval: TronReapproval? = null,
        ) : GaslessError()
        data object ModuleUpdateUnavailable : GaslessError()
        data object YieldBalanceUnavailable : GaslessError()
        data class DataError(val cause: Throwable?) : GaslessError()
    }

    /**
     * A new exact approval to [spenderAddress] for [amount] that leaves room for both the approval and the swap
     * compensations.
     */
    data class TronReapproval(val spenderAddress: String, val amount: BigDecimal)

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