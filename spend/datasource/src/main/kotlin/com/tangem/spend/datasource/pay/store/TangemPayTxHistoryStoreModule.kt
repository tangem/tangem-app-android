package com.tangem.spend.datasource.pay.store

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.tangem.datasource.utils.KotlinxDataStoreSerializer
import com.tangem.spend.datasource.pay.store.entity.TangemPayTxHistoryItemDM
import com.tangem.spend.datasource.pay.store.entity.TangemPayTxHistoryItemToDMConverter
import com.tangem.spend.datasource.pay.store.entity.TangemPayTxHistoryItemToDomainConverter
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TangemPayTxHistoryStoreModule {

    @Provides
    @Singleton
    fun provideTangemPayTxHistoryItemsStore(
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
        toDMConverter: TangemPayTxHistoryItemToDMConverter,
        toDomainConverter: TangemPayTxHistoryItemToDomainConverter,
    ): TangemPayTxHistoryItemsStore {
        return DefaultTangemPayTxHistoryItemsStore(
            dataStore = DataStoreFactory.create(
                serializer = KotlinxDataStoreSerializer(
                    defaultValue = emptyMap(),
                    serializer = MapSerializer(
                        keySerializer = String.serializer(),
                        valueSerializer = MapSerializer(
                            keySerializer = String.serializer(),
                            valueSerializer = ListSerializer(TangemPayTxHistoryItemDM.serializer()),
                        ),
                    ),
                    // TangemPayTxHistoryItemDM.Collateral declares a `type` field that would clash with the
                    // default kotlinx polymorphic class discriminator ("type"), so persist sealed subtypes
                    // under a non-conflicting discriminator key.
                    json = KotlinxDataStoreSerializer.jsonBuilder { classDiscriminator = "__type" },
                ),
                corruptionHandler = ReplaceFileCorruptionHandler { emptyMap() },
                produceFile = { context.dataStoreFile(fileName = "tangem_pay_tx_history") },
                scope = appScope,
            ),
            toDMConverter = toDMConverter,
            toDomainConverter = toDomainConverter,
        )
    }
}