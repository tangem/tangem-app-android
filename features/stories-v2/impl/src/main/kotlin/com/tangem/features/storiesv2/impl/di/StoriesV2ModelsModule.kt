package com.tangem.features.storiesv2.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.storiesv2.impl.model.StoriesV2Model
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface StoriesV2ModelsModule {

    @Binds
    @IntoMap
    @ClassKey(StoriesV2Model::class)
    fun bindStoriesV2Model(model: StoriesV2Model): Model
}