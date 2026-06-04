package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpressHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProviders(items: List<ExpressProviderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExchanges(items: List<ExpressExchangeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOnramps(items: List<ExpressOnrampEntity>)

    @Query(
        """
        SELECT *
        FROM express_exchange
        WHERE owner_address = :ownerAddress
        ORDER BY created_at DESC
        """,
    )
    fun observeExchanges(ownerAddress: String): Flow<List<ExpressExchangeEntity>>

    @Query(
        """
        SELECT *
        FROM express_onramp
        WHERE owner_address = :ownerAddress
        ORDER BY created_at DESC
        """,
    )
    fun observeOnramps(ownerAddress: String): Flow<List<ExpressOnrampEntity>>

    @Query(
        """
        SELECT *
        FROM express_exchange
        WHERE owner_address = :ownerAddress
          AND payin_hash = :hash
        LIMIT 1
        """,
    )
    suspend fun findExchangeByPayinHash(ownerAddress: String, hash: String): ExpressExchangeEntity?

    @Query(
        """
        SELECT *
        FROM express_exchange
        WHERE owner_address = :ownerAddress
          AND payout_hash = :hash
        LIMIT 1
        """,
    )
    suspend fun findExchangeByPayoutHash(ownerAddress: String, hash: String): ExpressExchangeEntity?

    @Query(
        """
        SELECT *
        FROM express_onramp
        WHERE owner_address = :ownerAddress
          AND payout_hash = :hash
        LIMIT 1
        """,
    )
    suspend fun findOnrampByPayoutHash(ownerAddress: String, hash: String): ExpressOnrampEntity?
}