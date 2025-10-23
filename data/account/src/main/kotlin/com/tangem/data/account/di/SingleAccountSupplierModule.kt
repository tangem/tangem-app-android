package com.tangem.data.account.di

import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.supplier.SingleAccountSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SingleAccountSupplierModule {

    @Provides
    @Singleton
    fun provideSingleAccountSupplier(factory: SingleAccountProducer.Factory): SingleAccountSupplier {
        return object : SingleAccountSupplier(
            factory = factory,
            keyCreator = { "single_account_${it.accountId.value}" },
        ) {}
    }
}