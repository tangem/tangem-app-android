package com.tangem.data.quotes.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.data.quotes.multi.DefaultMultiQuoteUpdater
import com.tangem.data.quotes.repository.DefaultQuotesRepository
import com.tangem.data.quotes.store.DefaultQuotesStatusesStore
import com.tangem.data.quotes.store.QuoteStatusDM
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteUpdater
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QuotesDataModule {

    @OptIn(ExperimentalStdlibApi::class)
    @Singleton
    @Provides
    fun provideQuotesStoreV2(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
    ): QuotesStatusesStore {
        return DefaultQuotesStatusesStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    defaultValue = QuoteStatusDM.Empty,
                    adapter = moshi.adapter<QuoteStatusDM>(),
                ),
                produceFile = { context.dataStoreFile(fileName = "quotes_v2") },
                scope = appScope,
            ),
            legacyCacheFile = context.dataStoreFile(fileName = "quotes"),
            scope = appScope,
        )
    }

    @Singleton
    @Provides
    fun providesQuotesRepository(quotesStatusesStore: QuotesStatusesStore): QuotesRepository {
        return DefaultQuotesRepository(quotesStatusesStore = quotesStatusesStore)
    }

    @Singleton
    @Provides
    fun bindMultiQuoteUpdater(
        appCurrencyResponseStore: AppCurrencyResponseStore,
        quotesStatusesStore: QuotesStatusesStore,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        coroutineScope: AppCoroutineScope,
    ): MultiQuoteUpdater {
        return DefaultMultiQuoteUpdater(
            appCurrencyResponseStore = appCurrencyResponseStore,
            quotesStatusesStore = quotesStatusesStore,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            coroutineScope = coroutineScope,
        )
    }

    @Singleton
    @Provides
    fun provideGetCurrencyUSDQuoteUseCase(quotesRepository: QuotesRepository): GetCurrencyUSDQuoteUseCase {
        return GetCurrencyUSDQuoteUseCase(quotesRepository)
    }
}