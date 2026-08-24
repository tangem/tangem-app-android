package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tangem.datasource.local.txhistory.db.entity.staking.P2PEthPoolVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface P2PEthPoolVaultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<P2PEthPoolVaultEntity>)

    @Query("SELECT * FROM p2p_eth_pool_vault")
    fun getAllAsFlow(): Flow<List<P2PEthPoolVaultEntity>>
}