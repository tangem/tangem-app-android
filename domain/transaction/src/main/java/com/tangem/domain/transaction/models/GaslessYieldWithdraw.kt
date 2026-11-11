package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import java.math.BigInteger

/**
 * Partial withdraw from the user's yield module that tops up the gasless fee token, executed in the same batch right
 * after the user's main transaction.
 *
 * @property amount        CEILING-rounded copy of the withdrawn amount, for DISPLAY only; it may exceed the executed
 *                         (floor-rounded) amount encoded in [callData] by ≤1 base unit, so never build call data from it
 * @property callData      `withdraw(yieldToken, amount)`, already upgrade-wrapped when the module needs an upgrade
 * @property moduleAddress the user's yield module, the target of [callData]
 */
data class GaslessYieldWithdraw(
    val amount: BigInteger,
    val callData: SmartContractCallData,
    val moduleAddress: String,
)