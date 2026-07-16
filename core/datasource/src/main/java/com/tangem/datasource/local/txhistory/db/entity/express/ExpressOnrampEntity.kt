package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.*

/**
 * Persisted representation of a single onramp transaction.
 *
 * Mirrors [com.tangem.datasource.api.onramp.models.response.OnrampItemResponse].
 */
@Entity(
    tableName = "express_onramp",
    indices = [
        // Incoming onramp lookup (observeIncomingOnramps): owner + to-asset equality, created_at range/sort.
        Index(value = ["owner_address", "to_network", "to_contract_address", "created_at"]),
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

    /** Address that received the target assets */
    @ColumnInfo(name = "payout_address")
    val payoutAddress: String,

    /**
     * Raw backend status string, persisted as-is (kept unparsed so a new value never breaks anything).
     * Typed view: [com.tangem.domain.express.models.ExpressOnrampStatus].
     */
    @ColumnInfo(name = "status")
    val status: String,

    /** Failure reason reported by the provider */
    @ColumnInfo(name = "fail_reason")
    val failReason: String?,

    /** External transaction ID reported by the provider in the webhook */
    @ColumnInfo(name = "external_tx_id")
    val externalTxId: String?,

    /** URL to view the transaction details on the provider side (not provided by all providers) */
    @ColumnInfo(name = "external_tx_url")
    val externalTxUrl: String?,

    /** Blockchain hash of the payout transaction */
    @ColumnInfo(name = "payout_hash")
    val payoutHash: String?,

    /** Transaction creation timestamp in ISO-8601 format */
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    /** Transaction last-update timestamp in ISO-8601 format */
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    /** Fiat currency code of the source funds */
    @ColumnInfo(name = "from_currency_code")
    val fromCurrencyCode: String,

    /** Fiat amount of the source funds */
    @ColumnInfo(name = "from_amount")
    val fromAmount: String,

    /** Number of decimal places of the source fiat currency */
    @ColumnInfo(name = "from_precision")
    val fromPrecision: Int,

    @Embedded(prefix = "to_")
    val to: AssetEmbedded,

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String,

    @ColumnInfo(name = "country_code")
    val countryCode: String,
) {

    data class AssetEmbedded(

        @ColumnInfo(name = "contract_address")
        val contractAddress: String,

        @ColumnInfo(name = "network")
        val network: String,

        @ColumnInfo(name = "decimals")
        val decimals: Int,

        /** Provider-promised amount. Present only if the provider reported it */
        @ColumnInfo(name = "amount")
        val amount: String?,

        /** Actual provider-confirmed amount delivered to the user */
        @ColumnInfo(name = "actual_amount")
        val actualAmount: String?,
    )
}