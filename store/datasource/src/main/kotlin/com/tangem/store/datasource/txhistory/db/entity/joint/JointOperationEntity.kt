package com.tangem.store.datasource.txhistory.db.entity.joint

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index

/**
 * Persisted joint account operation, in any status, as the operation history endpoint returns it.
 *
 * Keyed by the wallet too: two wallets of one user holding slots in the same joint account each see the operation,
 * each under its own [cryptoAccountId].
 */
@Entity(
    tableName = "joint_operation",
    primaryKeys = ["operation_id", "user_wallet_id"],
    indices = [
        // Finished operations of one account in one network, finished_at range/sort.
        Index(value = ["user_wallet_id", "crypto_account_id", "chain_id", "finished_at"]),
    ],
)
data class JointOperationEntity(

    @ColumnInfo(name = "operation_id")
    val operationId: String,

    @ColumnInfo(name = "user_wallet_id")
    val userWalletId: String,

    @ColumnInfo(name = "crypto_account_id", collate = ColumnInfo.NOCASE)
    val cryptoAccountId: String,

    @ColumnInfo(name = "chain_id")
    val chainId: Long,

    @ColumnInfo(name = "safe_address")
    val safeAddress: String,

    /** Raw backend status string, persisted as-is so a new value never breaks anything */
    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "initiator")
    val initiator: String?,

    /** ISO-8601 */
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    /** ISO-8601; `null` while the operation is in flight */
    @ColumnInfo(name = "finished_at")
    val finishedAt: String?,

    /** Hash of the relayer transaction; `null` while in flight, for a cancelled one and for a failure before the chain */
    @ColumnInfo(name = "tx_hash")
    val txHash: String?,

    @Embedded(prefix = "safe_tx_")
    val safeTx: SafeTxEmbedded,
) {

    data class SafeTxEmbedded(

        @ColumnInfo(name = "to")
        val to: String,

        @ColumnInfo(name = "value")
        val value: String,

        @ColumnInfo(name = "data")
        val data: String,

        @ColumnInfo(name = "operation")
        val operation: Int,

        @ColumnInfo(name = "safe_tx_gas")
        val safeTxGas: String,

        @ColumnInfo(name = "base_gas")
        val baseGas: String,

        @ColumnInfo(name = "gas_price")
        val gasPrice: String,

        @ColumnInfo(name = "gas_token")
        val gasToken: String,

        @ColumnInfo(name = "refund_receiver")
        val refundReceiver: String,

        @ColumnInfo(name = "nonce")
        val nonce: String,
    )
}