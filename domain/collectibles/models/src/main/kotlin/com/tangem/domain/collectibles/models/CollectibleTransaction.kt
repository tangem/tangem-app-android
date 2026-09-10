package com.tangem.domain.collectibles.models

/**
 * An unsigned collectible purchase transaction.
 *
 * @property txData base64-encoded transaction to be signed
 */
data class CollectibleTransaction(
    val id: String,
    val txData: String,
)

data class SubmittedCollectibleTransaction(val id: String)

/**
 * @property toAddress the pack is opened to this address instead of [fromAddress]
 */
data class GenerateTxParams(
    val fromAddress: String,
    val collectibleType: String,
    val toAddress: String,
    val collectibleTurboMode: CollectibleTurboMode? = null,
)

/**
 * @property buybackAddress auto-sell proceeds are sent here instead of the transaction's `fromAddress`
 */
data class CollectibleTurboMode(
    val isEnabled: Boolean,
    val buybackAddress: String? = null,
)