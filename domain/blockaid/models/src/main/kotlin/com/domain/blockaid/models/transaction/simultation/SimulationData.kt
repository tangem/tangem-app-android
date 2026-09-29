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
     * Represents an approve operation with the specified amount (can be multiple amounts for NFT).
     *
     * A single transaction can both grant an approval and move assets (e.g. a payable `approve` that also
     * transfers the native coin), so [send] and [receive] carry the balance changes simulated alongside it.
     */
    data class Approve(
        val items: List<ApproveInfo>,
        val send: List<AmountInfo> = emptyList(),
        val receive: List<AmountInfo> = emptyList(),
    ) : SimulationData()

    /**
     * Simulation was successfully performed and no changes detected
     */
    data object NoWalletChangesDetected : SimulationData()
}