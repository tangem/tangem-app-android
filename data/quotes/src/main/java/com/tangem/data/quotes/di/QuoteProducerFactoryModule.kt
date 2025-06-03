package com.tangem.data.quotes.di

import com.tangem.data.quotes.single.DefaultSingleQuoteProducer
import com.tangem.domain.quotes.single.SingleQuoteProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface QuoteProducerFactoryModule {

    @Binds
    @Singleton
    fun bindSingleQuoteProducerFactory(impl: DefaultSingleQuoteProducer.Factory): SingleQuoteProducer.Factory
}