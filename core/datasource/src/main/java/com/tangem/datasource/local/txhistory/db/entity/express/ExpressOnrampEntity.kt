package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.*

/**
 * Persisted representation of a single onramp transaction.
 *
 * Mirrors [com.tangem.datasource.api.onramp.models.response.OnrampItemResponse].
 */
@Entity(
    tableName = "express_onramp",
    foreignKeys = [
        ForeignKey(
            entity = ExpressProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["owner_address", "to_network", "created_at"]),
        Index(value = ["owner_address", "payout_hash"]),
    ],
)
data class ExpressOnrampEntity(

    @PrimaryKey
    @ColumnInfo(name = "tx_id")
    val txId: String,

    /**
     * Address used to query the history. For onramp it matches [payoutAddress].
     */
    @ColumnInfo(name = "owner_address")
    val ownerAddress: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    /** Address from which the source assets were taken for the exchange */
    @ColumnInfo(name = "from_address")
    val fromAddress: String,

    /** Address to which the assets were transferred for the exchange */
    @ColumnInfo(name = "payin_address")
    val payinAddress: String,

    /** Extra ID used for the pay-in transaction */
    @ColumnInfo(name = "payin_extra_id")
    val payinExtraId: String?,

    /** Address that received the target assets */
    @ColumnInfo(name = "payout_address")
    val payoutAddress: String,

    /** Refund destination address */
    @ColumnInfo(name = "refund_address")
    val refundAddress: String?,

    /** Extra ID used for refunds */
    @ColumnInfo(name = "refund_extra_id")
    val refundExtraId: String?,

    /**
     * fixed / float
     */
    @ColumnInfo(name = "rate_type")
    val rateType: String,

    /**
     * unknown
     * exchange-tx-sent
     * waiting
     * waiting-tx-hash
     * expired
     * confirming
     * exchanging
     * sending
     * finished
     * failed
     * tx-failed
     * refunded
     * verifying
     * paused
     */
    @ColumnInfo(name = "status")
    val status: String,

    /** External transaction ID (CEX only) */
    @ColumnInfo(name = "external_tx_id")
    val externalTxId: String?,

    /** Transaction status reported by the provider */
    @ColumnInfo(name = "external_tx_status")
    val externalTxStatus: String?,

    /** URL to view the transaction details (CEX only) */
    @ColumnInfo(name = "external_tx_url")
    val externalTxUrl: String?,

    /** Blockchain hash of the pay-in transaction */
    @ColumnInfo(name = "payin_hash")
    val payinHash: String?,

    /** Blockchain hash of the payout transaction */
    @ColumnInfo(name = "payout_hash")
    val payoutHash: String?,

    /** Network used for the refund transaction (when status is refunded) */
    @ColumnInfo(name = "refund_network")
    val refundNetwork: String?,

    /** Refunded token contract address */
    @ColumnInfo(name = "refund_contract_address")
    val refundContractAddress: String?,

    /** Transaction creation timestamp in ISO-8601 format */
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    /** Pay-in expiration timestamp in ISO-8601 format */
    @ColumnInfo(name = "pay_till")
    val payTill: String?,

    /** Average provider exchange duration in seconds */
    @ColumnInfo(name = "average_duration")
    val averageDuration: Long?,

    @Embedded(prefix = "from_")
    val from: AssetEmbedded,

    @Embedded(prefix = "to_")
    val to: AssetEmbedded,
) {

    data class AssetEmbedded(

        @ColumnInfo(name = "contract_address")
        val contractAddress: String,

        @ColumnInfo(name = "network")
        val network: String,

        @ColumnInfo(name = "decimals")
        val decimals: Int,

        @ColumnInfo(name = "amount")
        val amount: String,

        /** Actual provider-confirmed amount. Present only for the [ExpressOnrampEntity.to] asset */
        @ColumnInfo(name = "actual_amount")
        val actualAmount: String?,
    )
}