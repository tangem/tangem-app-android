package com.tangem.tap.di.domain

import com.tangem.domain.news.repository.NewsRepository
import com.tangem.domain.news.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NewsDomainModule {

    @Provides
    @Singleton
    fun provideGetNewsCategoriesUseCase(repository: NewsRepository): GetNewsCategoriesUseCase {
        return GetNewsCategoriesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideObserveNewsDetailsUseCase(repository: NewsRepository): ObserveNewsDetailsUseCase {
        return ObserveNewsDetailsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideObserveTrendingNewsUseCase(repository: NewsRepository): ManageTrendingNewsUseCase {
        return ManageTrendingNewsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetNewsListBatchFlowUseCase(repository: NewsRepository): GetNewsListBatchFlowUseCase {
        return GetNewsListBatchFlowUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideFetchTrendingNewsUseCase(repository: NewsRepository): FetchTrendingNewsUseCase {
        return FetchTrendingNewsUseCase(repository)
    }
}