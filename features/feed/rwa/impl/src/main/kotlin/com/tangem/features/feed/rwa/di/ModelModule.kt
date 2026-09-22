package com.tangem.features.feed.rwa.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.rwa.categories.model.CategorySelectorModel
import com.tangem.features.feed.rwa.model.RwaFeedTabModel
import com.tangem.features.feed.rwa.model.search.RwaFeedSearchTabModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(RwaFeedTabModel::class)
    fun bindRwaFeedTabModel(model: RwaFeedTabModel): Model

    @Binds
    @IntoMap
    @ClassKey(RwaFeedSearchTabModel::class)
    fun bindRwaFeedSearchTabModel(model: RwaFeedSearchTabModel): Model

    @Binds
    @IntoMap
    @ClassKey(CategorySelectorModel::class)
    fun bindCategorySelectorModel(model: CategorySelectorModel): Model
}