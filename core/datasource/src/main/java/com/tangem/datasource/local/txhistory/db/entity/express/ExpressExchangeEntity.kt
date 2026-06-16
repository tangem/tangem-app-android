package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.*

@Suppress("BooleanPropertyNaming")
@Entity(
    tableName = "express_exchange",
    foreignKeys = [
        ForeignKey(
            entity = ExpressProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["owner_address", "from_network", "updated_at"]),
        Index(value = ["owner_address", "payin_hash"]),
        Index(value = ["owner_address", "payout_hash"]),
        Index(value = ["owner_address", "refund_hash"]),
    ],
)
data class ExpressExchangeEntity(

    @PrimaryKey
    @ColumnInfo(name = "tx_id")
    val txId: String,

    @ColumnInfo(name = "owner_address")
    val ownerAddress: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    /**
     * waiting
     * confirming
     * exchanging
     * sending
     * finished
     * failed
     * refunded
     * expired
     */
    @ColumnInfo(name = "status")
    val status: String,

    @Embedded(prefix = "from_")
    val from: AssetEmbedded,

    @Embedded(prefix = "to_")
    val to: AssetEmbedded,

    /**
     * true -> actual provider-confirmed amount
     * false -> estimated amount
     */
    @ColumnInfo(name = "to_is_actual", defaultValue = "0")
    val toIsActual: Boolean,

    /**
     * Match key for PAYIN leg
     */
    @ColumnInfo(name = "payin_hash")
    val payinHash: String?,

    /**
     * Match key for PAYOUT leg
     */
    @ColumnInfo(name = "payout_hash")
    val payoutHash: String?,

    @ColumnInfo(name = "external_tx_id")
    val externalTxId: String?,

    @ColumnInfo(name = "external_tx_url")
    val externalTxUrl: String?,

    /**
     * fixed / float
     */
    @ColumnInfo(name = "rate_type")
    val rateType: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @Embedded(prefix = "refund_")
    val refund: RefundEmbedded?,
) {

    data class AssetEmbedded(

        @ColumnInfo(name = "network")
        val network: String,

        @ColumnInfo(name = "token_id")
        val tokenId: String?,

        @ColumnInfo(name = "raw_amount")
        val rawAmount: String,

        @ColumnInfo(name = "decimals")
        val decimals: Int,
    )

    data class RefundEmbedded(

        @ColumnInfo(name = "network")
        val network: String?,

        @ColumnInfo(name = "token_id")
        val tokenId: String?,

        @ColumnInfo(name = "raw_amount")
        val rawAmount: String?,

        @ColumnInfo(name = "decimals")
        val decimals: Int?,

        /**
         * Match key for REFUND leg
         */
        @ColumnInfo(name = "hash")
        val hash: String?,
    )
}