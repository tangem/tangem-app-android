package com.tangem.tap.di.domain

import com.tangem.domain.news.repository.NewsRepository
import com.tangem.domain.news.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object NewsDomainModule {

    @Provides
    fun provideGetNewsCategoriesUseCase(repository: NewsRepository): GetNewsCategoriesUseCase {
        return GetNewsCategoriesUseCase(repository)
    }

    @Provides
    fun provideObserveNewsDetailsUseCase(repository: NewsRepository): ObserveNewsDetailsUseCase {
        return ObserveNewsDetailsUseCase(repository)
    }

    @Provides
    fun provideObserveTrendingNewsUseCase(repository: NewsRepository): ManageTrendingNewsUseCase {
        return ManageTrendingNewsUseCase(repository)
    }

    @Provides
    fun provideGetNewsListBatchFlowUseCase(repository: NewsRepository): GetNewsListBatchFlowUseCase {
        return GetNewsListBatchFlowUseCase(repository)
    }

    @Provides
    fun provideFetchTrendingNewsUseCase(repository: NewsRepository): FetchTrendingNewsUseCase {
        return FetchTrendingNewsUseCase(repository)
    }

    @Provides
    fun provideMarkArticleAsViewedUseCase(repository: NewsRepository): MarkArticleAsViewedUseCase {
        return MarkArticleAsViewedUseCase(repository)
    }

    @Provides
    fun provideGetNewsUseCase(repository: NewsRepository): GetNewsUseCase {
        return GetNewsUseCase(repository)
    }
}