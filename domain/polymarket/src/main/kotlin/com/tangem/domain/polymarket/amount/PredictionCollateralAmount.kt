package com.tangem.domain.polymarket.amount

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.polymarket.approval.PolymarketContracts
import java.math.BigDecimal
import java.math.BigInteger

sealed interface PredictionAmountError {

    data object NotPositive : PredictionAmountError

    data object TooFine : PredictionAmountError
}

object PredictionCollateralAmount {

    /**
     * @return the amount as an integer of base units, or [PredictionAmountError.TooFine] when it carries
     *  more precision than one base unit — rounding it either submits zero or withdraws more than asked
     */
    fun toBaseUnits(amount: BigDecimal): Either<PredictionAmountError, BigInteger> {
        if (amount <= BigDecimal.ZERO) return PredictionAmountError.NotPositive.left()

        val scaled = amount.movePointRight(PolymarketContracts.COLLATERAL_DECIMALS)

        return runCatching { scaled.toBigIntegerExact() }.fold(
            onSuccess = { it.right() },
            onFailure = { PredictionAmountError.TooFine.left() },
        )
    }
}