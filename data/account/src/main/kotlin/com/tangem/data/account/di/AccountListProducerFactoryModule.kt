package com.tangem.data.account.di

import com.tangem.data.account.producer.DefaultMultiAccountListProducer
import com.tangem.data.account.producer.DefaultSingleAccountListProducer
import com.tangem.domain.account.producer.MultiAccountListProducer
import com.tangem.domain.account.producer.SingleAccountListProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountListProducerFactoryModule {

    @Binds
    @Singleton
    fun bindSingleAccountListProducerFactory(
        impl: DefaultSingleAccountListProducer.Factory,
    ): SingleAccountListProducer.Factory

    @Binds
    @Singleton
    fun bindMultiAccountListProducerFactory(
        impl: DefaultMultiAccountListProducer.Factory,
    ): MultiAccountListProducer.Factory
}