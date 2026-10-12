package com.tangem.grow.datasource.collectibles.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for POST /api/v1/collectible/generate-purchase-tx
 *
 * @property fromAddress the player for the gacha: the vault, which pays for the spin
 * @property toAddress the pack is opened to this address instead of [fromAddress]; the vault's owner on Day 1
 */
@Serializable
data class GeneratePurchaseTxRequestBody(
    @SerialName("fromAddress")
    val fromAddress: String,
    @SerialName("machineId")
    val machineId: String,
    @SerialName("toAddress")
    val toAddress: String,
    @SerialName("turboMode")
    val turboMode: TurboModeDTO? = null,
) {

    @Serializable
    data class TurboModeDTO(
        @SerialName("enabled")
        val isEnabled: Boolean = false,
        @SerialName("buybackAddress")
        val buybackAddress: String? = null,
    )
}