package com.tangem.features.feed.rwa.di

import com.tangem.features.feed.nav.FeedScreenFactory
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.components.CategorySelectorScreenFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface CategorySelectorModule {

    @Binds
    @IntoMap
    @ClassKey(CategorySelectorRoute::class)
    fun bindCategorySelectorScreenFactory(impl: CategorySelectorScreenFactory): FeedScreenFactory
}