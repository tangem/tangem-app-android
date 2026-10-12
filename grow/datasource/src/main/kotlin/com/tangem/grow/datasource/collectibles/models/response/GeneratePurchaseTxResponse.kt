package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response for POST /api/v1/collectible/generate-purchase-tx */
@Serializable
data class GeneratePurchaseTxResponse(
    @SerialName("txId")
    val txId: String,
    @SerialName("txData")
    val txData: TxDataDTO,
) {

    /**
     * @property memo `<slug>-<uuid>`, the gacha's key for this spin
     * @property fromAmount pack price in base units of [fromContractAddress]
     * @property feeAmount Tangem's fee on top of [fromAmount], same units
     */
    @Serializable
    data class TxDataDTO(
        @SerialName("memo")
        val memo: String,
        @SerialName("fromNetwork")
        val fromNetwork: String,
        @SerialName("fromContractAddress")
        val fromContractAddress: String,
        @SerialName("fromDecimals")
        val fromDecimals: Int,
        @SerialName("fromAmount")
        val fromAmount: String,
        @SerialName("feeAmount")
        val feeAmount: String,
    )
}