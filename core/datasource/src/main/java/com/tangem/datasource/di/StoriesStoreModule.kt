package com.tangem.datasource.di

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.local.stories.DefaultStoriesStore
import com.tangem.datasource.local.stories.StoriesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StoriesStoreModule {

    @Provides
    @Singleton
    fun provideStoriesStore(): StoriesStore {
        return DefaultStoriesStore(store = RuntimeSharedMapStore())
    }
}