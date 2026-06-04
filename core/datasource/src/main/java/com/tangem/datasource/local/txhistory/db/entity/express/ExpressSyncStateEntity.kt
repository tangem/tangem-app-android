package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Persisted sync state of the express tx history.
 *
 * Stored inside [com.tangem.datasource.local.txhistory.db.TxHistoryDatabase] on purpose: if the history tables are
 * dropped (e.g. destructive migration), the sync state is wiped together with them and the history is re-synced
 * from scratch.
 */
@Entity(
    tableName = "express_sync_state",
    primaryKeys = ["type", "address"],
)
data class ExpressSyncStateEntity(

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "is_initial_completed")
    val isInitialCompleted: Boolean,

    @ColumnInfo(name = "after_cursor")
    val afterCursor: String?,

    @ColumnInfo(name = "delta_cursor")
    val deltaCursor: String?,
) {

    enum class Type {
        EXCHANGE,
        ONRAMP,
    }
}