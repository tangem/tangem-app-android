package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Resolved strategy for paying a gasless transaction fee. Produced by ResolveGaslessFeePlanUseCase,
 * consumed by CreateAndSendEvmGaslessTransactionUseCase.
 */
sealed interface GaslessFeePlan {

    /** Pay in the native coin (enough native balance) — falls back to the standard fee. */
    data class NativePay(val fee: Fee) : GaslessFeePlan

    /**
     * Pay the fee in [feeToken]. [approval] and [yieldWithdraw] are independent sub-calls of one batch: the approve
     * runs before the user's transaction, which spends the allowance it grants, and the withdraw runs after it,
     * topping up the fee token before the relayer settles the fee. With neither, the payload is a single transaction.
     */
    data class TokenPay(
        val feeToken: CryptoCurrency.Token,
        val fee: Fee.Ethereum.TokenCurrency,
        val approval: GaslessApproval? = null,
        val yieldWithdraw: GaslessYieldWithdraw? = null,
    ) : GaslessFeePlan
}