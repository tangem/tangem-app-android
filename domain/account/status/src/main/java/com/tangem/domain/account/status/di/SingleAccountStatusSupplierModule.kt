package com.tangem.domain.account.status.di

import com.tangem.domain.account.status.producer.DefaultSingleAccountStatusProducer
import com.tangem.domain.account.status.producer.SingleAccountStatusProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusSupplier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SingleAccountStatusSupplierModule {

    @Binds
    @Singleton
    fun bindSingleAccountStatusProducerFactory(
        impl: DefaultSingleAccountStatusProducer.Factory,
    ): SingleAccountStatusProducer.Factory

    companion object {

        @Provides
        @Singleton
        fun provideSingleAccountStatusSupplier(
            factory: SingleAccountStatusProducer.Factory,
        ): SingleAccountStatusSupplier {
            return object : SingleAccountStatusSupplier(
                factory = factory,
                keyCreator = { "single_account_status_${it.accountId.value}" },
            ) {}
        }
    }
}