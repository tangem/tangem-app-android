package com.tangem.datasource.local.txhistory.db.entity.staking

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted representation of a single P2P.org pooled-staking vault.
 *
 * Mirrors [com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolVaultDTO].
 */
@Entity(tableName = "p2p_eth_pool_vault")
data class P2PEthPoolVaultEntity(

    @PrimaryKey
    @ColumnInfo(name = "vault_address")
    val vaultAddress: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "apy")
    val apy: String,

    @ColumnInfo(name = "base_apy")
    val baseApy: String,

    @ColumnInfo(name = "capacity")
    val capacity: String,

    @ColumnInfo(name = "total_assets")
    val totalAssets: String,

    @ColumnInfo(name = "fee_percent")
    val feePercent: String,

    @ColumnInfo(name = "is_private")
    val isPrivate: Boolean,

    @ColumnInfo(name = "is_genesis")
    val isGenesis: Boolean,

    @ColumnInfo(name = "is_smoothing_pool")
    val isSmoothingPool: Boolean,

    @ColumnInfo(name = "is_erc20")
    val isErc20: Boolean,

    @ColumnInfo(name = "token_name")
    val tokenName: String?,

    @ColumnInfo(name = "token_symbol")
    val tokenSymbol: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)