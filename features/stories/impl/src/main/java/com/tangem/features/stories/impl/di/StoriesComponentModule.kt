package com.tangem.features.stories.impl.di

import com.tangem.features.stories.api.component.StoriesComponent
import com.tangem.features.stories.impl.DefaultStoriesComponent
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
    fun bindStoriesComponentFactory(factory: DefaultStoriesComponent.Factory): StoriesComponent.Factory
}