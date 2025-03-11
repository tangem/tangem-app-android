package com.tangem.feature.stories.impl.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.stories.impl.model.StoriesModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface StoriesModelModule {

    @Binds
    @IntoMap
    @ClassKey(StoriesModel::class)
    fun bindStoriesModel(model: StoriesModel): Model
}