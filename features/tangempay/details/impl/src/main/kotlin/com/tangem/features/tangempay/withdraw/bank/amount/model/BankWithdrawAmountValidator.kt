package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.tangem.utils.extensions.isZero
import java.math.BigDecimal

internal object BankWithdrawAmountValidator {

    sealed interface Result {
        data object Empty : Result
        data object Valid : Result
        data object OutOfRange : Result
        data object NotEnoughFunds : Result
    }

    fun validate(
        amount: BigDecimal?,
        minAmount: BigDecimal,
        maxAmount: BigDecimal,
        balance: BigDecimal,
        fee: BigDecimal,
    ): Result = when {
        amount == null || amount.isZero() -> Result.Empty
        amount < minAmount || amount > maxAmount -> Result.OutOfRange
        amount + fee > balance -> Result.NotEnoughFunds
        else -> Result.Valid
    }

    /** The most the user can send: balance minus the rail fee, capped by [maxAmount], never negative. */
    fun sendableAmount(maxAmount: BigDecimal, balance: BigDecimal, fee: BigDecimal): BigDecimal =
        (balance - fee).min(maxAmount).max(BigDecimal.ZERO)
}