package com.tangem.store.datasource.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationVoteEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationWithVotes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface JointOperationDao {

    /** Stores the operations and replaces their votes with the ones they carry */
    @Transaction
    suspend fun upsert(items: List<JointOperationWithVotes>) {
        upsertOperations(items.map(JointOperationWithVotes::operation))
        items.groupBy { it.operation.userWalletId }.forEach { (userWalletId, walletItems) ->
            deleteVotes(userWalletId = userWalletId, operationIds = walletItems.map { it.operation.operationId })
        }
        insertVotes(items.flatMap(JointOperationWithVotes::votes))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOperations(items: List<JointOperationEntity>)

    @Query("DELETE FROM joint_operation_vote WHERE user_wallet_id = :userWalletId AND operation_id IN (:operationIds)")
    suspend fun deleteVotes(userWalletId: String, operationIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVotes(items: List<JointOperationVoteEntity>)

    /**
     * Operations of one joint account in one network that are in one of [statuses] and finished no earlier than
     * [fromFinishedAtIso] (ISO-8601 sorts lexicographically == chronologically), newest first.
     */
    fun observe(
        userWalletId: String,
        cryptoAccountId: String,
        chainId: Long,
        statuses: List<String>,
        fromFinishedAtIso: String,
    ): Flow<List<JointOperationWithVotes>> = observeWithVotes(
        userWalletId = userWalletId,
        cryptoAccountId = cryptoAccountId,
        chainId = chainId,
        statuses = statuses,
        fromFinishedAtIso = fromFinishedAtIso,
    ).map(::toOperationsWithVotes)

    @Query(
        """
        SELECT *
        FROM joint_operation AS operation
        LEFT JOIN joint_operation_vote AS vote
          ON vote.user_wallet_id = operation.user_wallet_id AND vote.operation_id = operation.operation_id
        WHERE operation.user_wallet_id = :userWalletId
          AND operation.crypto_account_id = :cryptoAccountId
          AND operation.chain_id = :chainId
          AND operation.status IN (:statuses)
          AND operation.finished_at >= :fromFinishedAtIso
        ORDER BY operation.finished_at DESC
        """,
    )
    fun observeWithVotes(
        userWalletId: String,
        cryptoAccountId: String,
        chainId: Long,
        statuses: List<String>,
        fromFinishedAtIso: String,
    ): Flow<Map<JointOperationEntity, List<JointOperationVoteEntity>>>

    @Query(
        """
        SELECT operation_id
        FROM joint_operation
        WHERE user_wallet_id = :userWalletId
          AND status IN (:statuses)
        """,
    )
    fun observeIds(userWalletId: String, statuses: List<String>): Flow<List<String>>
}

private fun toOperationsWithVotes(
    rows: Map<JointOperationEntity, List<JointOperationVoteEntity>>,
): List<JointOperationWithVotes> = rows.map { (operation, votes) -> JointOperationWithVotes(operation, votes) }