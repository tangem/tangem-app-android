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
        UPDATE express_sync_state
        SET after_cursor = :afterCursor,
            is_initial_completed = :isInitialCompleted
        WHERE type = :type
          AND address = :address
        """,
    )
    suspend fun updateHistoryCursor(type: String, address: String, afterCursor: String?, isInitialCompleted: Boolean)

    @Query(
        """
        UPDATE express_sync_state
        SET delta_cursor = :deltaCursor
        WHERE type = :type
          AND address = :address
        """,
    )
    suspend fun updateDeltaCursor(type: String, address: String, deltaCursor: String)

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