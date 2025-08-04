package com.domain.blockaid.models.transaction.simultation

/**
 * Result of a successful transaction simulation.
 */
sealed class SimulationData {

    /**
     * Represents a swap/send/sell operations with specified send and receive amounts (can be multiple amounts for NFT)
     */
    data class SendAndReceive(
        val send: List<AmountInfo>,
        val receive: List<AmountInfo>,
    ) : SimulationData()

    /**
     * Represents an approve operation with the specified amount (can be multiple amounts for NFT)
     */
    data class Approve(
        val approvedAmounts: List<ApprovedAmount>,
    ) : SimulationData()

    /**
     * Simulation was successfully performed and no changes detected
     */
    data object NoWalletChangesDetected : SimulationData()
}