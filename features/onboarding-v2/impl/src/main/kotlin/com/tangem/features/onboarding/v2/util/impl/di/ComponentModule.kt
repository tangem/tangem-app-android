package com.tangem.features.onboarding.v2.util.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.util.ResetCardsComponent
import com.tangem.features.onboarding.v2.util.impl.DefaultResetCardsComponent
import com.tangem.features.onboarding.v2.util.impl.model.ResetCardsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindResetCardsComponent(factory: DefaultResetCardsComponent.Factory): ResetCardsComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {
    @Binds
    @IntoMap
    @ClassKey(ResetCardsModel::class)
    fun provideModel(model: ResetCardsModel): Model
}