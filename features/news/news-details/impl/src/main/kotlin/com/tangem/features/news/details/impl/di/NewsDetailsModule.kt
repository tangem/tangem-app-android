package com.tangem.features.news.details.impl.di

import com.tangem.features.news.details.api.NewsDetailsComponent
import com.tangem.features.news.details.impl.DefaultNewsDetailsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NewsDetailsModule {

    @Binds
    @Singleton
    fun bindNewsDetailsComponentFactory(factory: DefaultNewsDetailsComponent.Factory): NewsDetailsComponent.Factory
}