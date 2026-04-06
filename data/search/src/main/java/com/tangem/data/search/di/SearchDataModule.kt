package com.tangem.data.search.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.data.search.model.SearchHistoryDTO
import com.tangem.data.search.repository.DefaultSearchRepository
import com.tangem.data.search.store.DefaultSearchHistoryStore
import com.tangem.data.search.store.SearchHistoryStore
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.search.repository.SearchRepository
import com.tangem.domain.search.usecase.ClearSearchHistoryUseCase
import com.tangem.domain.search.usecase.GetSearchResultsUseCase
import com.tangem.domain.search.usecase.SaveRecentSearchTokenUseCase
import com.tangem.domain.search.usecase.SaveSearchQueryUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SearchDataModule {

    @OptIn(ExperimentalStdlibApi::class)
    @Provides
    @Singleton
    fun provideSearchHistoryDataStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
    ): DataStore<SearchHistoryDTO> {
        return DataStoreFactory.create(
            serializer = MoshiDataStoreSerializer(
                defaultValue = SearchHistoryDTO(),
                adapter = moshi.adapter<SearchHistoryDTO>(),
            ),
            produceFile = { context.dataStoreFile(fileName = "search_history") },
            scope = appScope,
        )
    }

    @Provides
    @Singleton
    fun provideSearchHistoryStore(dataStore: DataStore<SearchHistoryDTO>): SearchHistoryStore {
        return DefaultSearchHistoryStore(dataStore = dataStore)
    }

    @Provides
    @Singleton
    fun provideSearchRepository(
        store: SearchHistoryStore,
        dispatchers: CoroutineDispatcherProvider,
    ): SearchRepository {
        return DefaultSearchRepository(
            store = store,
            dispatchers = dispatchers,
        )
    }

    @Provides
    fun provideGetSearchResultsUseCase(
        searchRepository: SearchRepository,
        multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
        userWalletsListRepository: UserWalletsListRepository,
    ): GetSearchResultsUseCase {
        return GetSearchResultsUseCase(
            searchRepository = searchRepository,
            multiAccountStatusListSupplier = multiAccountStatusListSupplier,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    fun provideSaveSearchQueryUseCase(searchRepository: SearchRepository): SaveSearchQueryUseCase {
        return SaveSearchQueryUseCase(searchRepository = searchRepository)
    }

    @Provides
    fun provideSaveRecentSearchTokenUseCase(searchRepository: SearchRepository): SaveRecentSearchTokenUseCase {
        return SaveRecentSearchTokenUseCase(searchRepository = searchRepository)
    }

    @Provides
    fun provideClearSearchHistoryUseCase(searchRepository: SearchRepository): ClearSearchHistoryUseCase {
        return ClearSearchHistoryUseCase(searchRepository = searchRepository)
    }
}