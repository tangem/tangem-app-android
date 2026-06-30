package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tangem.datasource.local.txhistory.db.entity.express.TokenInfoEntity

@Dao
interface TokenInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<TokenInfoEntity>)

    /**
     * Cached tokens for the given networks/contracts. Filters each column independently, so the result is a
     * cross-product superset of the `(networkId, contractAddress)` pairs — the caller must match exact pairs.
     * Contract match is case-insensitive; pass [minUpdatedAt] = `now - ttl` to drop stale rows.
     */
    @Query(
        """
        SELECT * FROM token_info
        WHERE network_id IN (:networkIds)
          AND contract_address COLLATE NOCASE IN (:contractAddresses)
          AND updated_at >= :minUpdatedAt
        """,
    )
    suspend fun getCached(
        networkIds: Collection<String>,
        contractAddresses: Collection<String>,
        minUpdatedAt: Long = 0,
    ): List<TokenInfoEntity>
}