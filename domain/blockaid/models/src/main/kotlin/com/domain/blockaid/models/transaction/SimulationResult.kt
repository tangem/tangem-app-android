package com.domain.blockaid.models.transaction

import com.domain.blockaid.models.transaction.simultation.SimulationData

/**
 * Result of BlockAid's transaction simulation
 */
sealed class SimulationResult {

    /**
     * Simulation was successfully performed and returned data
     */
    data class Success(
        val data: SimulationData,
    ) : SimulationResult()

    /**
     * Simulation wasn't performed, BlockAid cannot guarantee transaction's behavior
     */
    data object FailedToSimulate : SimulationResult()
}