package com.tangem.features.storiesv2.impl.di

import com.tangem.features.storiesv2.StoriesV2Component
import com.tangem.features.storiesv2.StoriesV2Prefetcher
import com.tangem.features.storiesv2.StoriesV2StorybookComponent
import com.tangem.features.storiesv2.impl.component.DefaultStoriesV2Component
import com.tangem.features.storiesv2.impl.content.DefaultStoryV2ContentRepository
import com.tangem.features.storiesv2.impl.content.StoryV2ContentRepository
import com.tangem.features.storiesv2.impl.content.remote.DefaultStoryV2AssetStore
import com.tangem.features.storiesv2.impl.content.remote.OkHttpStoryV2FileDownloader
import com.tangem.features.storiesv2.impl.content.remote.StoryV2AssetStore
import com.tangem.features.storiesv2.impl.content.remote.StoryV2FileDownloader
import com.tangem.features.storiesv2.impl.prefetch.DefaultStoriesV2Prefetcher
import com.tangem.features.storiesv2.impl.storybook.DefaultStoriesV2StorybookComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface StoriesV2FeatureModule {

    @Binds
    fun bindFactory(impl: DefaultStoriesV2Component.Factory): StoriesV2Component.Factory

    @Binds
    fun bindStorybookFactory(impl: DefaultStoriesV2StorybookComponent.Factory): StoriesV2StorybookComponent.Factory

    @Binds
    @Singleton
    fun bindPrefetcher(impl: DefaultStoriesV2Prefetcher): StoriesV2Prefetcher

    @Binds
    @Singleton
    fun bindContentRepository(impl: DefaultStoryV2ContentRepository): StoryV2ContentRepository

    @Binds
    @Singleton
    fun bindAssetStore(impl: DefaultStoryV2AssetStore): StoryV2AssetStore

    @Binds
    @Singleton
    fun bindFileDownloader(impl: OkHttpStoryV2FileDownloader): StoryV2FileDownloader
}