package com.tangem.feature.stories.impl.di

import com.tangem.feature.stories.api.StoriesComponent
import com.tangem.feature.stories.impl.DefaultStoriesComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface StoriesComponentModule {

    @Binds
    @Singleton
    fun bindSwapStoriesComponentFactory(factory: DefaultStoriesComponent.Factory): StoriesComponent.Factory
}