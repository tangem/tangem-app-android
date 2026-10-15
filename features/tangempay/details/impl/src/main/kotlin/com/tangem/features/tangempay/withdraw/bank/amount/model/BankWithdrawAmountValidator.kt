package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.tangem.utils.extensions.isPositive
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

    /**
     * What Max fills in: balance minus the rail fee, capped by [maxAmount]. A balance that does not cover the fee is
     * filled in whole, so the amount error explains why it cannot be sent.
     */
    fun maxInputAmount(maxAmount: BigDecimal, balance: BigDecimal, fee: BigDecimal): BigDecimal {
        val balanceAfterFee = balance - fee
        val amount = if (balanceAfterFee.isPositive()) balanceAfterFee else balance
        return amount.min(maxAmount).max(BigDecimal.ZERO)
    }
}