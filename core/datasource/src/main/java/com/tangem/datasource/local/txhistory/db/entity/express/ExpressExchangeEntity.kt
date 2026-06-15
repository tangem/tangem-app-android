package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.*

/**
 * Persisted representation of a single exchange transaction.
 *
 * Mirrors [com.tangem.datasource.api.express.models.response.ExchangeItemResponse].
 */
@Entity(
    tableName = "express_exchange",
    indices = [
        // Outgoing swaps lookup (observeOutgoingSwaps): owner + from-asset equality, created_at range/sort.
        Index(value = ["owner_address", "from_network", "from_contract_address", "created_at"]),
        // Incoming (cross-owner) swaps lookup (observeIncomingSwaps): to-asset equality, created_at range/sort.
        // No owner filter here, so to_contract_address in the index is what keeps a popular to-network selective.
        Index(value = ["to_network", "to_contract_address", "created_at"]),
    ],
)
data class ExpressExchangeEntity(

    @PrimaryKey
    @ColumnInfo(name = "tx_id")
    val txId: String,

    /**
     * Address used to query the history. For exchange it matches [fromAddress].
     */
    @ColumnInfo(name = "owner_address")
    val ownerAddress: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    /**
     * Address from which the `from` assets were taken for the exchange. Optional because the very first
     * app versions did not send it; for newer versions it can be considered effectively mandatory.
     */
    @ColumnInfo(name = "from_address")
    val fromAddress: String?,

    /** Address to which the source assets were transferred for the exchange */
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
     * Raw backend status string, persisted as-is (kept unparsed so a new value never breaks anything).
     * Typed view: [com.tangem.domain.express.models.ExpressExchangeStatus].
     */
    @ColumnInfo(name = "status")
    val status: String,

    /** External transaction ID (CEX only) */
    @ColumnInfo(name = "external_tx_id")
    val externalTxId: String?,

    /** URL to view the transaction details (CEX only) */
    @ColumnInfo(name = "external_tx_url")
    val externalTxUrl: String?,

    /** Blockchain hash of the pay-in transaction */
    @ColumnInfo(name = "payin_hash")
    val payinHash: String?,

    /** Blockchain hash of the payout transaction */
    @ColumnInfo(name = "payout_hash")
    val payoutHash: String?,

    /** Network used for the refund transaction */
    @ColumnInfo(name = "refund_network")
    val refundNetwork: String?,

    /** Refunded token contract address */
    @ColumnInfo(name = "refund_contract_address")
    val refundContractAddress: String?,

    /** Transaction creation timestamp in ISO-8601 format */
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    /** Transaction last-update timestamp in ISO-8601 format */
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

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

        /** Actual provider-confirmed amount. Present only for the [ExpressExchangeEntity.to] asset */
        @ColumnInfo(name = "actual_amount")
        val actualAmount: String?,
    )
}