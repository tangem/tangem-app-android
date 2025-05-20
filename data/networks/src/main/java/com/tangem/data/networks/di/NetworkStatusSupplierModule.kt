package com.tangem.data.networks.di

import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
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
    fun provideSingleNetworkStatusSupplier(factory: SingleNetworkStatusProducer.Factory): SingleNetworkStatusSupplier {
        return object : SingleNetworkStatusSupplier(
            factory = factory,
            keyCreator = {
                "single_network_status_${it.userWalletId.stringValue}_${it.network.rawId}_" +
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