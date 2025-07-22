package com.domain.blockaid.models.transaction

/**
 * Result of BlockAid's transaction check
 *
 * @property validation Indicates whether the transaction is considered safe or unsafe
 * @property simulation Provides insight into the expected outcome of the transaction
 */
data class CheckTransactionResult(
    val validation: ValidationResult,
    val description: String? = null,
    val simulation: SimulationResult,
)