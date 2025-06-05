package com.tangem.data.quotes.di

import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QuoteSupplierModule {

    @Provides
    @Singleton
    fun provideSingleQuoteSupplier(factory: SingleQuoteProducer.Factory): SingleQuoteSupplier {
        return object : SingleQuoteSupplier(
            factory = factory,
            keyCreator = { "single_quote_${it.rawCurrencyId.value}" },
        ) {}
    }
}