package com.tangem.domain.models.currency.balance

import java.math.BigDecimal

/**
 * An extra balance that keeps growing between two reads. Kept generic so a screen can extrapolate it without knowing
 * the feature behind it.
 */
interface AccruingBalanceContribution : BalanceContribution {

    /** Yearly growth of [totalDeltaCryptoAmount] as a fraction; zero while unknown */
    val accrualRate: BigDecimal
}