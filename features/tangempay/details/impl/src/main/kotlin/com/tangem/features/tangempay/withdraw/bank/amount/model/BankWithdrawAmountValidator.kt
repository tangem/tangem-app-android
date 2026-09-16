package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.tangem.utils.extensions.isZero
import java.math.BigDecimal

internal object BankWithdrawAmountValidator {

    sealed interface Result {
        data object Empty : Result
        data object Valid : Result
        data object BelowMinimum : Result
        data object NotEnoughFunds : Result
    }

    fun validate(amount: BigDecimal?, minAmount: BigDecimal, balance: BigDecimal, fee: BigDecimal): Result = when {
        amount == null || amount.isZero() -> Result.Empty
        amount < minAmount -> Result.BelowMinimum
        amount + fee > balance -> Result.NotEnoughFunds
        else -> Result.Valid
    }

    /** The most the user can send: the selected token's balance minus the rail fee, never negative. */
    fun maxAmount(balance: BigDecimal, fee: BigDecimal): BigDecimal = (balance - fee).max(BigDecimal.ZERO)
}