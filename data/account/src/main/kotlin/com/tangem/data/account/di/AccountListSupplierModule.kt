package com.tangem.data.account.di

import com.tangem.domain.account.producer.MultiAccountListProducer
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountListSupplierModule {

    @Provides
    @Singleton
    fun provideSingleAccountListSupplier(factory: SingleAccountListProducer.Factory): SingleAccountListSupplier {
        return object : SingleAccountListSupplier(
            factory = factory,
            keyCreator = { "single_account_list_${it.userWalletId.stringValue}" },
        ) {}
    }

    @Provides
    @Singleton
    fun provideMultiNetworkStatusSupplier(factory: MultiAccountListProducer.Factory): MultiAccountListSupplier {
        return object : MultiAccountListSupplier(
            factory = factory,
            keyCreator = { "multi_networks_statuses" },
        ) {}
    }
}