package com.tangem.data.quotes.di

import com.tangem.data.quotes.multi.DefaultMultiQuoteStatusProducer
import com.tangem.data.quotes.single.DefaultSingleQuoteStatusProducer
import com.tangem.domain.quotes.multi.MultiQuoteStatusProducer
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

    @Binds
    @Singleton
    fun bindMultiQuoteStatusProducerFactory(
        impl: DefaultMultiQuoteStatusProducer.Factory,
    ): MultiQuoteStatusProducer.Factory
}