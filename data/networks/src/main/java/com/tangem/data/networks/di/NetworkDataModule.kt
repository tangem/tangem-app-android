package com.tangem.data.networks.di

import androidx.datastore.core.DataStore
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.repository.DefaultNetworksRepository
import com.tangem.data.networks.store.DefaultNetworksStatusesStoreV2
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkDataModule {

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