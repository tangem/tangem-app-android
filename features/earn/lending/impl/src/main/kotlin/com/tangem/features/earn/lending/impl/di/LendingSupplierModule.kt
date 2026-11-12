package com.tangem.features.earn.lending.impl.di

import com.tangem.features.earn.lending.impl.supplier.DefaultLendingFetcher
import com.tangem.features.earn.lending.impl.supplier.DefaultLendingProducer
import com.tangem.features.earn.lending.impl.supplier.LendingFetcher
import com.tangem.features.earn.lending.impl.supplier.LendingProducer
import com.tangem.features.earn.lending.impl.supplier.LendingSupplier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface LendingSupplierBindsModule {

    @Binds
    fun bindProducerFactory(impl: DefaultLendingProducer.Factory): LendingProducer.Factory

    @Binds
    fun bindFetcher(impl: DefaultLendingFetcher): LendingFetcher
}

@Module
@InstallIn(SingletonComponent::class)
internal object LendingSupplierModule {

    @Provides
    @Singleton
    fun provideLendingSupplier(factory: LendingProducer.Factory): LendingSupplier = LendingSupplier(
        factory = factory,
        keyCreator = { params -> "lending_${params.userWalletId.stringValue}" },
    )
}