package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.*

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
        Index(value = ["owner_address", "to_network", "updated_at"]),
        Index(value = ["owner_address", "payout_hash"]),
    ],
)
data class ExpressOnrampEntity(

    @PrimaryKey
    @ColumnInfo(name = "tx_id")
    val txId: String,

    @ColumnInfo(name = "owner_address")
    val ownerAddress: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    /**

     * waiting-for-payment
     * payment-processing
     * paused
     * verifying
     * sending
     * finished
     * failed
     * expired
     * refunded
     */
    @ColumnInfo(name = "status")
    val status: String,

    /**
     * ISO-4217
     */
    @ColumnInfo(name = "from_currency_code")
    val fromCurrencyCode: String,

    /**
     * Decimal string
     */
    @ColumnInfo(name = "from_amount")
    val fromAmount: String,

    @ColumnInfo(name = "to_network")
    val toNetwork: String,

    @ColumnInfo(name = "to_token_id")
    val toTokenId: String?,

    /**
     * Estimated amount at creation moment
     */
    @ColumnInfo(name = "to_expected_raw_amount")
    val toExpectedRawAmount: String,

    /**
     * Actual provider-confirmed amount
     */
    @ColumnInfo(name = "to_actual_raw_amount")
    val toActualRawAmount: String?,

    @ColumnInfo(name = "to_decimals")
    val toDecimals: Int,

    /**
     * Match key with gateway_tx.hash
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

    @ColumnInfo(name = "fail_reason")
    val failReason: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @Embedded(prefix = "refund_")
    val refund: RefundEmbedded?,
) {

    data class RefundEmbedded(

        /**
         * ISO-4217
         */
        @ColumnInfo(name = "currency_code")
        val currencyCode: String?,

        @ColumnInfo(name = "amount")
        val amount: String?,
    )
}