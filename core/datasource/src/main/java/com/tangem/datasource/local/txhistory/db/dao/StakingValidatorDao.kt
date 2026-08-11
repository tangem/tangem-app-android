package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tangem.datasource.local.txhistory.db.entity.staking.StakingValidatorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StakingValidatorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<StakingValidatorEntity>)

    @Query("SELECT * FROM staking_validator")
    fun getAllAsFlow(): Flow<List<StakingValidatorEntity>>
}