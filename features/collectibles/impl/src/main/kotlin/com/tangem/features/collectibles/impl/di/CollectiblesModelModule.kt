package com.tangem.features.collectibles.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.collectibles.impl.onboarding.model.CollectiblesOnboardingModel
import com.tangem.features.collectibles.impl.stories.model.CollectiblesStoriesModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface CollectiblesModelModule {

    @Binds
    @IntoMap
    @ClassKey(CollectiblesOnboardingModel::class)
    fun bindCollectiblesOnboardingModel(impl: CollectiblesOnboardingModel): Model

    @Binds
    @IntoMap
    @ClassKey(CollectiblesStoriesModel::class)
    fun bindCollectiblesStoriesModel(impl: CollectiblesStoriesModel): Model
}