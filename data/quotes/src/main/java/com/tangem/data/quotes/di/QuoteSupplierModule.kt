package com.tangem.data.quotes.di

import androidx.datastore.core.DataStore
import com.tangem.data.quotes.store.CurrencyIdWithQuote
import com.tangem.data.quotes.store.DefaultQuotesStoreV2
import com.tangem.data.quotes.store.QuotesStoreV2
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    fun provideQuotesStoreV2(
        persistenceQuotesStore: DataStore<CurrencyIdWithQuote>,
        dispatchers: CoroutineDispatcherProvider,
    ): QuotesStoreV2 {
        return DefaultQuotesStoreV2(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = persistenceQuotesStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideSingleQuoteSupplier(factory: SingleQuoteProducer.Factory): SingleQuoteSupplier {
        return object : SingleQuoteSupplier(
            factory = factory,
            keyCreator = { "single_quote_${it.rawCurrencyId.value}" },
        ) {}
    }
}