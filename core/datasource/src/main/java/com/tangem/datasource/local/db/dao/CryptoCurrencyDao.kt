package com.tangem.datasource.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tangem.datasource.local.db.entity.CryptoCurrencyEntity
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoCurrencyDao {

    @Insert
    suspend fun insert(currencies: List<CryptoCurrencyEntity>)

    @Query(
        """
            SELECT * FROM CryptoCurrencyEntity 
            WHERE userWalletId = :userWalletId AND accountId = :accountId
        """,
    )
    suspend fun selectByAccountId(userWalletId: UserWalletId, accountId: Int): List<CryptoCurrencyEntity>

    @Query(
        """
            SELECT * FROM CryptoCurrencyEntity 
            WHERE userWalletId = :userWalletId AND accountId = :accountId
        """,
    )
    fun observeByAccountId(userWalletId: UserWalletId, accountId: Int): Flow<List<CryptoCurrencyEntity>>

    @Query(
        """
            SELECT COUNT(*) FROM CryptoCurrencyEntity 
            WHERE userWalletId = :userWalletId
        """,
    )
    suspend fun countByUserWalletId(userWalletId: UserWalletId): Int
}