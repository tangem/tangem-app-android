package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.tangem.datasource.local.txhistory.db.TxHistoryDatabase
import com.tangem.datasource.local.txhistory.store.CommonSyncState
import com.tangem.datasource.local.txhistory.store.CommonSyncStateKey
import com.tangem.datasource.local.txhistory.store.DefaultTxHistoryStore
import com.tangem.datasource.local.txhistory.store.TxHistoryStore
import com.tangem.datasource.utils.KotlinxDataStoreSerializer
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.MapSerializer
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
            ).build()
        }

        @Provides
        @Singleton
        fun provideTxHistoryStore(@ApplicationContext context: Context, appScope: AppCoroutineScope): TxHistoryStore {
            val commonSerializer = KotlinxDataStoreSerializer(
                defaultValue = emptyMap(),
                serializer = MapSerializer(
                    CommonSyncStateKey.serializer(),
                    CommonSyncState.serializer(),
                ),
            )

            val expressExchangeStore = DataStoreFactory.create(
                serializer = commonSerializer,
                produceFile = { context.dataStoreFile(fileName = "TxHistoryExpressExchangeStore") },
                scope = appScope,
            )
            val expressOnrampStore = DataStoreFactory.create(
                serializer = commonSerializer,
                produceFile = { context.dataStoreFile(fileName = "TxHistoryExpressOnrampStore") },
                scope = appScope,
            )

            return DefaultTxHistoryStore(
                expressExchangeStore = expressExchangeStore,
                expressOnrampStore = expressOnrampStore,
            )
        }
    }
}