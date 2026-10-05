package com.tangem.data.search.di

import androidx.datastore.core.DataStore
import com.tangem.data.search.model.SearchHistoryDTO
import com.tangem.data.search.repository.DefaultSearchRepository
import com.tangem.data.search.store.DefaultSearchHistoryStore
import com.tangem.data.search.store.SearchHistoryStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.search.repository.SearchRepository
import com.tangem.domain.search.usecase.ClearSearchHistoryUseCase
import com.tangem.domain.search.usecase.GetSearchResultsUseCase
import com.tangem.domain.search.usecase.SaveRecentSearchTokenUseCase
import com.tangem.domain.search.usecase.SaveSearchQueryUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SearchDataModule {

    @Provides
    @Singleton
    fun provideSearchHistoryDataStore(dataStoreFactory: AppDataStoreFactory): DataStore<SearchHistoryDTO> {
        return dataStoreFactory.create(defaultValue = SearchHistoryDTO(), fileName = "search_history")
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