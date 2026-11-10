package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Collectible Transaction params, the common answer of /api/v1/collectible/status, /purchase-tx-sent, /open-pack and
 * /active-txs. Only the fields the app reads are declared.
 *
 * @property machine present when `includeMachine` is set
 * @property packOpeningErrorCode why the pack ended in `pack-opening-error`; not in the swagger yet
 * @property nftWon present once the pack is opened
 * @property autoBuyback present when Turbo mode sold the card back right on opening and `includeAutoBuyback` is set
 */
@Serializable
data class CollectibleTxResponse(
    @SerialName("txId")
    val txId: String,
    @SerialName("status")
    val status: CollectibleTxStatusDTO = CollectibleTxStatusDTO.UNKNOWN,
    @SerialName("machine")
    val machine: MachineSummaryDTO? = null,
    @SerialName("turboModeEnabled")
    val isTurboModeEnabled: Boolean = false,
    @SerialName("packOpeningErrorCode")
    val packOpeningErrorCode: String? = null,
    @SerialName("nftWon")
    val nftWon: NftWonDTO? = null,
    @SerialName("autoBuyback")
    val autoBuyback: AutoBuybackDTO? = null,
) {

    @Serializable
    data class MachineSummaryDTO(
        @SerialName("externalId")
        val externalId: String,
        @SerialName("name")
        val name: String,
        @SerialName("shortName")
        val shortName: String? = null,
    )

    /**
     * @property toNetwork `solana` or `solana-dev`, by environment
     * @property toAmount the payout in base units of the token, [toDecimals] digits
     */
    @Serializable
    data class AutoBuybackDTO(
        @SerialName("buybackId")
        val buybackId: String,
        @SerialName("nftAddress")
        val nftAddress: String,
        @SerialName("fromAddress")
        val fromAddress: String,
        @SerialName("toNetwork")
        val toNetwork: String,
        @SerialName("toContractAddress")
        val toContractAddress: String,
        @SerialName("toDecimals")
        val toDecimals: Int,
        @SerialName("toAmount")
        val toAmount: String,
        @SerialName("toAddress")
        val toAddress: String,
        @SerialName("buybackTxHash")
        val buybackTxHash: String? = null,
        @SerialName("buybackedAt")
        val buybackedAt: String? = null,
        @SerialName("status")
        val status: BuybackStatusResponse.BuybackStatusDTO = BuybackStatusResponse.BuybackStatusDTO.UNKNOWN,
        @SerialName("updatedAt")
        val updatedAt: String,
        @SerialName("createdAt")
        val createdAt: String,
    )

    @Serializable
    enum class CollectibleTxStatusDTO {
        @SerialName("created")
        CREATED,

        @SerialName("waiting-tx-hash")
        WAITING_TX_HASH,

        @SerialName("purchase-tx-sent")
        PURCHASE_TX_SENT,

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