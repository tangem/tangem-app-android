package com.tangem.datasource.local.txhistory.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Unified pagination index over the local history sources.
 */
@Entity(
    tableName = "history_index",
    // A single row (type + entity_id) may be shown under more than one address (e.g. a swap between two owned tokens
    // appears under both), so the address is part of the identity.
    primaryKeys = ["type", "entity_id", "address"],
    indices = [
        // Newest-first cursor scan within an address: address filter + sort-time ordering + entity_id tie-break.
        Index(value = ["address", "sort_time_millis", "entity_id"]),
    ],
)
data class HistoryIndexEntity(

    @ColumnInfo(name = "type")
    val type: String,

    /** ID of the row in its own table. */
    @ColumnInfo(name = "entity_id")
    val entityId: String,

    /** Address the row is loaded under on the token-details screen. */
    @ColumnInfo(name = "address")
    val address: String,

    /** Time the unified timeline is sorted by (newest first). */
    @ColumnInfo(name = "sort_time_millis")
    val sortTimeMillis: Long,
) {

    enum class Type(val value: String) {
        EXCHANGE(value = "EXCHANGE"),
        ONRAMP(value = "ONRAMP"),
    }
}