package com.tangem.features.welcome.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.welcome.WelcomeComponent
import com.tangem.features.welcome.impl.DefaultWelcomeComponent
import com.tangem.features.welcome.impl.model.WelcomeModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureModule {

    @Binds
    fun bindComponentFactory(impl: DefaultWelcomeComponent.Factory): WelcomeComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(WelcomeModel::class)
    fun provideModel(model: WelcomeModel): Model
}