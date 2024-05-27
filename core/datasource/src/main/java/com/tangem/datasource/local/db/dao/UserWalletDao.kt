package com.tangem.datasource.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tangem.datasource.local.db.entity.UserWalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWalletDao {

    @Insert
    suspend fun insert(vararg wallets: UserWalletEntity)

    @Update
    suspend fun update(wallet: UserWalletEntity)

    @Query("SELECT * FROM UserWalletEntity")
    suspend fun selectAll(): List<UserWalletEntity>

    @Query("SELECT * FROM UserWalletEntity")
    fun observeAll(): Flow<List<UserWalletEntity>>

    @Query("SELECT * FROM UserWalletEntity WHERE id = :id")
    suspend fun selectById(id: String): UserWalletEntity?

    @Query("SELECT * FROM UserWalletEntity WHERE id = :id")
    fun observeById(id: String): Flow<UserWalletEntity>
}