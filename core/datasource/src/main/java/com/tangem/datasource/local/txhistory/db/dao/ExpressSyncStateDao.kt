package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpressSyncStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExpressSyncStateEntity)

    @Query(
        """
        SELECT *
        FROM express_sync_state
        WHERE type = :type
          AND address = :address
        LIMIT 1
        """,
    )
    fun observe(type: String, address: String): Flow<ExpressSyncStateEntity?>
}