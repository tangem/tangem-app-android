package com.domain.blockaid.models.transaction

import java.math.BigInteger

sealed interface GasEstimationResult {

    data class Estimated(val gasLimits: List<BigInteger>) : GasEstimationResult

    /** The simulation says a transaction would revert on chain */
    data object Reverted : GasEstimationResult

    data object Unavailable : GasEstimationResult
}