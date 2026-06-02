package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigInteger

/**
 * Resolved strategy for paying a gasless transaction fee. Produced by ResolveGaslessFeePlanUseCase,
 * consumed by CreateAndSendGaslessTransactionUseCase.
 */
sealed interface GaslessFeePlan {

    /** Pay in the native coin (enough native balance) — falls back to the standard fee. */
    data class NativePay(val fee: Fee) : GaslessFeePlan

    /** Pay the fee from the token's plain balance. */
    data class TokenPay(
        val feeToken: CryptoCurrency.Token,
        val fee: Fee.Ethereum.TokenCurrency,
    ) : GaslessFeePlan

    /**
     * Pay the fee by first withdrawing [withdrawAmount] of the token from the user's yield module
     * (appended as a second batch transaction). [withdrawCallData] is already upgrade-wrapped when
     * the module needs an upgrade.
     */
    data class TokenPayWithYieldWithdraw(
        val feeToken: CryptoCurrency.Token,
        val fee: Fee.Ethereum.TokenCurrency,
        val withdrawAmount: BigInteger,
        val withdrawCallData: SmartContractCallData,
        val yieldModuleAddress: String,
    ) : GaslessFeePlan
}