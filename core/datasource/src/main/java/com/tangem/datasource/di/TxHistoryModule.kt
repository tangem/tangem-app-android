package com.tangem.datasource.di

import android.content.Context
import androidx.room.Room
import com.tangem.datasource.local.txhistory.db.TxHistoryDatabase
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.dao.TokenInfoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TxHistoryModule {

    companion object {

        private const val TX_HISTORY_DATABASE_NAME = "tx_history_database.db"

        @Provides
        @Singleton
        fun provideTxHistoryDatabase(@ApplicationContext context: Context): TxHistoryDatabase {
            return Room.databaseBuilder(
                context = context,
                klass = TxHistoryDatabase::class.java,
                name = TX_HISTORY_DATABASE_NAME,
            )
                .fallbackToDestructiveMigration(true)
                .build()
        }

        @Provides
        fun provideExpressHistoryDao(database: TxHistoryDatabase): ExpressHistoryDao = database.expressHistoryDao()

        @Provides
        fun provideSyncStateDao(database: TxHistoryDatabase): ExpressSyncStateDao = database.syncStateDao()

        @Provides
        fun provideTokenInfoDao(database: TxHistoryDatabase): TokenInfoDao = database.tokenInfoDao()
    }
}