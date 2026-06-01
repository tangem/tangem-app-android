package com.tangem.domain.yield.supply.promo.usecase

import java.math.BigDecimal

/**
 * Pure boosted APY calculation. Hard-coded x3 coefficient — single place to swap when the backend
 * starts returning the coefficient explicitly.
 */
class GetBoostedApyUseCase {

    operator fun invoke(baseApy: BigDecimal): BigDecimal = baseApy.multiply(BOOST_MULTIPLIER)

    private companion object {
        val BOOST_MULTIPLIER: BigDecimal = BigDecimal(3)
    }
}