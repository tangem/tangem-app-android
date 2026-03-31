package com.tangem.domain.transaction.models

import java.math.BigDecimal

/**
 * Model that represents the allowance information for a specific spender and required amount.
 */
sealed class AllowanceInfo {

    /**
     * Represents a state where the current allowance is sufficient to cover the required amount.
     */
    data class Enough(val allowance: BigDecimal) : AllowanceInfo()

    /**
     * Represents a state where the current allowance is insufficient to cover the required amount.
     */
    data class NotEnough(val allowance: BigDecimal, val requiredAmount: BigDecimal) : AllowanceInfo()

    /**
     * Represents a state where the current allowance is insufficient,
     * but it must be reset to cover the required amount (specific to certain tokens like Tether in Ethereum).
     */
    data class ResetNeeded(val allowance: BigDecimal, val requiredAmount: BigDecimal) : AllowanceInfo()
}