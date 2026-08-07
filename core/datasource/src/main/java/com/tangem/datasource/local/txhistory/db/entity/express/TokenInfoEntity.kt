package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Cached token data fetched from [TangemTechApi.getCoins], keyed by network id + contract address.
 */
@Entity(
    tableName = "token_info",
    primaryKeys = ["network_id", "contract_address"],
)
data class TokenInfoEntity(

    @ColumnInfo(name = "network_id")
    val networkId: String,

    @ColumnInfo(name = "contract_address")
    val contractAddress: String,

    /** Coin id from the backend (used as the token's raw currency id and for the icon URL). */
    @ColumnInfo(name = "coin_id")
    val coinId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "symbol")
    val symbol: String,

    @ColumnInfo(name = "decimals")
    val decimals: Int,

    /** Last refresh timestamp in epoch milliseconds, used to evict stale entries. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)