package com.tangem.data.quotes.di

import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QuoteStatusSupplierModule {

    @Provides
    @Singleton
    fun provideSingleQuoteStatusSupplier(factory: SingleQuoteStatusProducer.Factory): SingleQuoteStatusSupplier {
        return object : SingleQuoteStatusSupplier(
            factory = factory,
            keyCreator = { "single_quote_${it.rawCurrencyId.value}" },
        ) {}
    }
}