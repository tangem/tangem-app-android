package com.tangem.data.networks.di

import androidx.datastore.core.DataStore
import com.tangem.data.networks.store.DefaultNetworksStatusesStoreV2
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkStatusSupplierModule {

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
    fun provideSingleNetworkStatusSupplier(factory: SingleNetworkStatusProducer.Factory): SingleNetworkStatusSupplier {
        return object : SingleNetworkStatusSupplier(
            factory = factory,
            keyCreator = {
                "single_network_status_${it.userWalletId.stringValue}_${it.network.id.value}_" +
                    it.network.derivationPath.value
            },
        ) {}
    }

    @Provides
    @Singleton
    fun provideMultiNetworkStatusSupplier(factory: MultiNetworkStatusProducer.Factory): MultiNetworkStatusSupplier {
        return object : MultiNetworkStatusSupplier(
            factory = factory,
            keyCreator = { "multi_networks_statuses_${it.userWalletId.stringValue}" },
        ) {}
    }
}