package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Collectible Transaction params, the common answer of /api/v1/collectible/status, /purchase-tx-sent and /open-pack.
 * Only the fields the app reads are declared.
 *
 * @property nftWon present once the pack is opened
 */
@Serializable
data class CollectibleTxResponse(
    @SerialName("txId")
    val txId: String,
    @SerialName("status")
    val status: CollectibleTxStatusDTO = CollectibleTxStatusDTO.UNKNOWN,
    @SerialName("nftWon")
    val nftWon: NftWonDTO? = null,
) {

    @Serializable
    enum class CollectibleTxStatusDTO {
        @SerialName("created")
        CREATED,

        @SerialName("waiting-tx-hash")
        WAITING_TX_HASH,

        @SerialName("purchase-tx-hash")
        PURCHASE_TX_HASH,

        @SerialName("waiting-for-payment")
        WAITING_FOR_PAYMENT,

        @SerialName("pack-purchased")
        PACK_PURCHASED,

        @SerialName("pack-opening-error")
        PACK_OPENING_ERROR,

        @SerialName("pack-opened")
        PACK_OPENED,

        @SerialName("nft-received")
        NFT_RECEIVED,

        @SerialName("auto-buybacked")
        AUTO_BUYBACKED,

        @SerialName("manually-buybacked")
        MANUALLY_BUYBACKED,

        @SerialName("expired")
        EXPIRED,

        @SerialName("refunded")
        REFUNDED,

        @SerialName("paused")
        PAUSED,

        /** Not a wire value: a status this build does not know is coerced to this default by `NetworkJsonFactory`. */
        UNKNOWN,
    }

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
        val gradingId: String?,
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