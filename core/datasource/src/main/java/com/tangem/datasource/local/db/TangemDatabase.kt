package com.tangem.datasource.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tangem.datasource.local.db.dao.CryptoCurrenciesAccountDao
import com.tangem.datasource.local.db.dao.CryptoCurrencyDao
import com.tangem.datasource.local.db.dao.UserWalletDao
import com.tangem.datasource.local.db.entity.CryptoCurrenciesAccountEntity
import com.tangem.datasource.local.db.entity.CryptoCurrencyEntity
import com.tangem.datasource.local.db.entity.UserWalletEntity
import com.tangem.datasource.local.db.utils.Converters

@Database(
    entities = [
        UserWalletEntity::class,
        CryptoCurrenciesAccountEntity::class,
        CryptoCurrencyEntity::class,
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class TangemDatabase : RoomDatabase() {

    abstract fun userWalletDao(): UserWalletDao

    abstract fun cryptoCurrencyDao(): CryptoCurrencyDao

    abstract fun cryptoCurrenciesAccountDao(): CryptoCurrenciesAccountDao
}