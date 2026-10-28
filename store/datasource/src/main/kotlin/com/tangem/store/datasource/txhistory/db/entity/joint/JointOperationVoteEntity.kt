package com.tangem.store.datasource.txhistory.db.entity.joint

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * A member's answer to a joint account operation, keyed by the wallet like the operation itself: the votes of one
 * wallet always come from the same response as its operation row.
 */
@Entity(
    tableName = "joint_operation_vote",
    primaryKeys = ["operation_id", "user_wallet_id", "address"],
)
data class JointOperationVoteEntity(

    @ColumnInfo(name = "operation_id")
    val operationId: String,

    @ColumnInfo(name = "user_wallet_id")
    val userWalletId: String,

    /** Owner address of the member, lowercase */
    @ColumnInfo(name = "address")
    val address: String,

    /** Raw backend vote string, persisted as-is so a new value never breaks anything */
    @ColumnInfo(name = "vote")
    val vote: String,

    /** ISO-8601 */
    @ColumnInfo(name = "voted_at")
    val votedAt: String?,
)