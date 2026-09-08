package com.tangem.features.feed.rwa.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.rwa.categories.model.CategorySelectorModel
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
    @ClassKey(CategorySelectorModel::class)
    fun bindCategorySelectorModel(model: CategorySelectorModel): Model
}