package com.tangem.datasource.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tangem.datasource.local.db.entity.CryptoCurrenciesAccountEntity
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoCurrenciesAccountDao {

    @Insert
    suspend fun insert(account: CryptoCurrenciesAccountEntity)

    @Update
    suspend fun update(account: CryptoCurrenciesAccountEntity)

    @Query("SELECT * FROM CryptoCurrenciesAccountEntity WHERE userWalletId = :userWalletId")
    fun observeByUserWalletId(userWalletId: UserWalletId): Flow<List<CryptoCurrenciesAccountEntity>>

    @Query("SELECT * FROM CryptoCurrenciesAccountEntity WHERE userWalletId = :userWalletId")
    suspend fun selectByUserWalletId(userWalletId: UserWalletId): List<CryptoCurrenciesAccountEntity>
}