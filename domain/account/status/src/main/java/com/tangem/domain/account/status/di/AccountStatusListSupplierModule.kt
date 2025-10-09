package com.tangem.domain.account.status.di

import com.tangem.domain.account.status.producer.MultiAccountStatusListProducer
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountStatusListSupplierModule {

    @Provides
    @Singleton
    fun provideSingleAccountStatusListSupplier(
        factory: SingleAccountStatusListProducer.Factory,
    ): SingleAccountStatusListSupplier {
        return object : SingleAccountStatusListSupplier(
            factory = factory,
            keyCreator = { "account_status_list_${it.userWalletId}" },
        ) {}
    }

    @Provides
    @Singleton
    fun provideMultiAccountStatusListSupplier(
        factory: MultiAccountStatusListProducer.Factory,
    ): MultiAccountStatusListSupplier {
        return object : MultiAccountStatusListSupplier(
            factory = factory,
            keyCreator = { "multi_account_status_list" },
        ) {}
    }
}