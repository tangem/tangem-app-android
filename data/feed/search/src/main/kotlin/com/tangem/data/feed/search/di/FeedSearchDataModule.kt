package com.tangem.data.feed.search.di

import com.tangem.data.feed.search.model.FeedSearchHistoryDTO
import com.tangem.data.feed.search.repository.DefaultFeedSearchHistoryRepository
import com.tangem.data.feed.search.store.DefaultFeedSearchHistoryStore
import com.tangem.data.feed.search.store.FeedSearchHistoryStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository
import com.tangem.domain.feed.search.usecase.ClearFeedSearchHistoryUseCase
import com.tangem.domain.feed.search.usecase.GetRecentFeedSearchItemsUseCase
import com.tangem.domain.feed.search.usecase.GetRecentFeedSearchQueriesUseCase
import com.tangem.domain.feed.search.usecase.RemoveFeedSearchQueryUseCase
import com.tangem.domain.feed.search.usecase.SaveFeedSearchQueryUseCase
import com.tangem.domain.feed.search.usecase.SaveRecentFeedSearchItemUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeedSearchDataModule {

    @Provides
    @Singleton
    fun provideFeedSearchHistoryStore(dataStoreFactory: AppDataStoreFactory): FeedSearchHistoryStore {
        return DefaultFeedSearchHistoryStore(
            dataStore = dataStoreFactory.create(
                defaultValue = FeedSearchHistoryDTO(),
                fileName = "feed_search_history",
            ),
        )
    }

    @Provides
    @Singleton
    fun provideFeedSearchHistoryRepository(
        store: FeedSearchHistoryStore,
        dispatchers: CoroutineDispatcherProvider,
    ): FeedSearchHistoryRepository {
        return DefaultFeedSearchHistoryRepository(store = store, dispatchers = dispatchers)
    }

    @Provides
    fun provideGetRecentFeedSearchItemsUseCase(
        repository: FeedSearchHistoryRepository,
    ): GetRecentFeedSearchItemsUseCase {
        return GetRecentFeedSearchItemsUseCase(repository = repository)
    }

    @Provides
    fun provideGetRecentFeedSearchQueriesUseCase(
        repository: FeedSearchHistoryRepository,
    ): GetRecentFeedSearchQueriesUseCase {
        return GetRecentFeedSearchQueriesUseCase(repository = repository)
    }

    @Provides
    fun provideSaveRecentFeedSearchItemUseCase(
        repository: FeedSearchHistoryRepository,
    ): SaveRecentFeedSearchItemUseCase {
        return SaveRecentFeedSearchItemUseCase(repository = repository)
    }

    @Provides
    fun provideSaveFeedSearchQueryUseCase(repository: FeedSearchHistoryRepository): SaveFeedSearchQueryUseCase {
        return SaveFeedSearchQueryUseCase(repository = repository)
    }

    @Provides
    fun provideRemoveFeedSearchQueryUseCase(repository: FeedSearchHistoryRepository): RemoveFeedSearchQueryUseCase {
        return RemoveFeedSearchQueryUseCase(repository = repository)
    }

    @Provides
    fun provideClearFeedSearchHistoryUseCase(repository: FeedSearchHistoryRepository): ClearFeedSearchHistoryUseCase {
        return ClearFeedSearchHistoryUseCase(repository = repository)
    }
}