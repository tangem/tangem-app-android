package com.tangem.data.quotes.di

import com.tangem.data.quotes.single.DefaultSingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface QuoteStatusProducerFactoryModule {

    @Binds
    @Singleton
    fun bindSingleQuoteStatusProducerFactory(
        impl: DefaultSingleQuoteStatusProducer.Factory,
    ): SingleQuoteStatusProducer.Factory
}