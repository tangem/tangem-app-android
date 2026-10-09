package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Collectible Transaction params, the common answer of /api/v1/collectible/status, /purchase-tx-sent and /open-pack.
 * Only the fields the app reads are declared.
 *
 * @property status kebab-case lifecycle state, e.g. `pack-purchased`
 * @property nftWon present once the pack is opened
 */
@Serializable
data class CollectibleTxResponse(
    @SerialName("txId")
    val txId: String,
    @SerialName("status")
    val status: String,
    @SerialName("nftWon")
    val nftWon: NftWonDTO? = null,
) {

    /** @property insuredValue USD * 100 */
    @Serializable
    data class NftWonDTO(
        @SerialName("nftAddress")
        val nftAddress: String,
        @SerialName("gradingCompany")
        val gradingCompany: String?,
        @SerialName("theGrade")
        val grade: String?,
        @SerialName("gradingID")
        val gradingId: String,
        @SerialName("imageFront")
        val imageFront: String?,
        @SerialName("imageBack")
        val imageBack: String?,
        @SerialName("rarity")
        val rarity: String?,
        @SerialName("year")
        val year: String?,
        @SerialName("set")
        val set: String?,
        @SerialName("serialNumber")
        val serialNumber: String?,
        @SerialName("insuredValue")
        val insuredValue: Long?,
    )
}