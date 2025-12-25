package com.tangem.features.news.list.impl.di

import com.tangem.features.news.list.api.NewsListComponent
import com.tangem.features.news.list.impl.DefaultNewsListComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NewsListModule {

    @Binds
    @Singleton
    fun bindNewsListComponentFactory(factory: DefaultNewsListComponent.Factory): NewsListComponent.Factory
}