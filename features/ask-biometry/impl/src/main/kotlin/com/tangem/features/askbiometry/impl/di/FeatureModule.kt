package com.tangem.features.askbiometry.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.askbiometry.AskBiometryComponent
import com.tangem.features.askbiometry.AskBiometryFeatureToggles
import com.tangem.features.askbiometry.impl.DefaultAskBiometryComponent
import com.tangem.features.askbiometry.impl.DefaultAskBiometryFeatureToggles
import com.tangem.features.askbiometry.impl.model.AskBiometryModel
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
    fun bindFeatureToggles(impl: DefaultAskBiometryFeatureToggles): AskBiometryFeatureToggles

    @Binds
    fun bindComponentFactory(impl: DefaultAskBiometryComponent.Factory): AskBiometryComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(AskBiometryModel::class)
    fun provideModel(model: AskBiometryModel): Model
}