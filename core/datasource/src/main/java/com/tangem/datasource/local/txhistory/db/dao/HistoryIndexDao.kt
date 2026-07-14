package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tangem.datasource.local.txhistory.db.entity.HistoryIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryIndexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<HistoryIndexEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HistoryIndexEntity)

    /**
     * One page of the unified timeline for [addresses] (usually one, but some token-details screens span several),
     * newest first, **one row per operation**. An operation indexed under several of the queried [addresses] (e.g. a
     * swap under both its from- and payout-address) is collapsed via `GROUP BY (type, entity_id)`, keeping its
     * most-recent occurrence (SQLite bare-column rule under a single `MAX`). This both de-duplicates the timeline and
     * makes the keyset key (`sort_time_millis`, `entity_id`) unique, so rows are neither skipped nor duplicated across
     * page boundaries even when several addresses are queried.
     *
     * The cursor is the (`sort_time_millis`, `entity_id`) of the last (oldest) row of the previous page — pass both
     * [cursorSortTimeMillis] and [cursorEntityId], or `null` for the first page.
     */
    @Query(
        """
        SELECT type, entity_id, address, MAX(sort_time_millis) AS sort_time_millis FROM history_index
        WHERE address IN (:addresses)
        GROUP BY type, entity_id
        HAVING (
            :cursorSortTimeMillis IS NULL
            OR MAX(sort_time_millis) < :cursorSortTimeMillis
            OR (MAX(sort_time_millis) = :cursorSortTimeMillis AND entity_id < :cursorEntityId)
        )
        ORDER BY sort_time_millis DESC, entity_id DESC
        LIMIT :limit
        """,
    )
    fun observePage(
        addresses: List<String>,
        cursorSortTimeMillis: Long?,
        cursorEntityId: String?,
        limit: Int,
    ): Flow<List<HistoryIndexEntity>>

    fun observePage(addresses: List<String>, cursor: Cursor?, limit: Int): Flow<List<HistoryIndexEntity>> = observePage(
        addresses = addresses,
        cursorSortTimeMillis = cursor?.sortTimeMillis,
        cursorEntityId = cursor?.entityId,
        limit = limit,
    )

    /**
     * Keyset cursor for [observePage]: the (sortTimeMillis, entityId) of the last (oldest) row of a page. Build it from
     * the previous page's last row to fetch the next page; a `null` cursor requests the first page.
     */
    data class Cursor(
        val sortTimeMillis: Long,
        val entityId: String,
    ) {

        companion object {
            fun from(lastRow: HistoryIndexEntity): Cursor = Cursor(
                sortTimeMillis = lastRow.sortTimeMillis,
                entityId = lastRow.entityId,
            )
        }
    }
}