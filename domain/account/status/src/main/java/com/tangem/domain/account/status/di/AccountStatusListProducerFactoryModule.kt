package com.tangem.domain.account.status.di

import com.tangem.domain.account.status.producer.*
import com.tangem.domain.core.flow.FlowProducerScope
import com.tangem.domain.core.flow.FlowProducerTools
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

    @Binds
    @Singleton
    fun bindDefaultFlowProducerTools(impl: DefaultFlowProducerTools): FlowProducerTools

    @Binds
    @Singleton
    fun bindFlowProducerScope(impl: DefaultFlowProducerAppScope): FlowProducerScope
}