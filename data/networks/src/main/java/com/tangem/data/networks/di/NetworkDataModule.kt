package com.tangem.data.networks.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.repository.DefaultNetworksRepository
import com.tangem.data.networks.store.DefaultNetworksStatusesStoreV2
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.datasource.utils.setTypes
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkDataModule {

    @Singleton
    @Provides
    fun providePersistenceNetworksStatusesStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): DataStore<Map<String, Set<NetworkStatusDM>>> {
        return DataStoreFactory.create(
            serializer = MoshiDataStoreSerializer(
                moshi = moshi,
                types = mapWithStringKeyTypes(valueTypes = setTypes<NetworkStatusDM>()),
                defaultValue = emptyMap(),
            ),
            produceFile = { context.dataStoreFile(fileName = "networks_statuses") },
            scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
        )
    }

    @Provides
    @Singleton
    fun provideNetworksStatusesStoreV2(
        persistenceNetworksStatusesStore: DataStore<Map<String, Set<NetworkStatusDM>>>,
        dispatchers: CoroutineDispatcherProvider,
    ): NetworksStatusesStoreV2 {
        return DefaultNetworksStatusesStoreV2(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = persistenceNetworksStatusesStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideNetworkRepository(
        cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
        walletManagersFacade: WalletManagersFacade,
        networksStatusesStoreV2: NetworksStatusesStoreV2,
        dispatchers: CoroutineDispatcherProvider,
    ): NetworksRepository {
        return DefaultNetworksRepository(
            cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
            walletManagersFacade = walletManagersFacade,
            networksStatusesStore = networksStatusesStoreV2,
            dispatchers = dispatchers,
        )
    }
}