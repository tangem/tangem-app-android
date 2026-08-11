package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.tangem.datasource.local.txhistory.db.entity.staking.StakingValidatorEntity

@Dao
interface StakingValidatorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<StakingValidatorEntity>)
}