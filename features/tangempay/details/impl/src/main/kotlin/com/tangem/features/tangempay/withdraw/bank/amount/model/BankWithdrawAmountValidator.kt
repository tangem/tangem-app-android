package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.tangem.domain.pay.model.bankofframp.AmountLimits
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal

internal object BankWithdrawAmountValidator {

    sealed interface Result {
        data object Empty : Result
        data object Valid : Result
        data object OutOfRange : Result
        data object NotEnoughFunds : Result
    }

    fun validate(amount: BigDecimal?, limits: AmountLimits, balance: BigDecimal, fee: BigDecimal): Result = when {
        amount == null || amount.isZero() -> Result.Empty
        amount < limits.min || amount > limits.max -> Result.OutOfRange
        amount + fee > balance -> Result.NotEnoughFunds
        else -> Result.Valid
    }

    /** The most the user can send: balance minus the rail fee, capped by the upper limit, never negative. */
    fun maxAmount(limits: AmountLimits, balance: BigDecimal, fee: BigDecimal): BigDecimal =
        (balance - fee).min(limits.max).max(BigDecimal.ZERO)
}