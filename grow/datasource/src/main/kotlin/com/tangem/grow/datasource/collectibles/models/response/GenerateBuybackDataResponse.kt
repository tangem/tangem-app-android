package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response for POST /api/v1/buyback/buyback-data */
@Serializable
data class GenerateBuybackDataResponse(
    @SerialName("buybackId")
    val buybackId: String,
    @SerialName("txData")
    val txData: TxDataDTO,
) {

    /**
     * @property toAmount the payout in base units of the received token, [toDecimals] digits
     * @property tx the transaction built by the gacha, base64; the owner's signature slot is still empty
     */
    @Serializable
    data class TxDataDTO(
        @SerialName("memo")
        val memo: String,
        @SerialName("toDecimals")
        val toDecimals: Int,
        @SerialName("toAmount")
        val toAmount: String,
        @SerialName("tx")
        val tx: String,
    )
}