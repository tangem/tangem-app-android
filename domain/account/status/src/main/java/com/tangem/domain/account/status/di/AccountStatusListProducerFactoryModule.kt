package com.tangem.domain.account.status.di

import com.tangem.domain.account.status.producer.DefaultMultiAccountStatusListProducer
import com.tangem.domain.account.status.producer.DefaultSingleAccountStatusListProducer
import com.tangem.domain.account.status.producer.MultiAccountStatusListProducer
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountStatusListProducerFactoryModule {

    @Binds
    @Singleton
    fun bindSingleAccountStatusListProducerFactory(
        factory: DefaultSingleAccountStatusListProducer.Factory,
    ): SingleAccountStatusListProducer.Factory

    @Binds
    @Singleton
    fun bindMultiAccountStatusListProducerFactory(
        factory: DefaultMultiAccountStatusListProducer.Factory,
    ): MultiAccountStatusListProducer.Factory
}