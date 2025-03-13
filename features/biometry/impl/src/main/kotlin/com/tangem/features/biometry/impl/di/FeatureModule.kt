package com.tangem.features.biometry.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.biometry.BiometryFeatureToggles
import com.tangem.features.biometry.impl.DefaultAskBiometryComponent
import com.tangem.features.biometry.impl.DefaultBiometryFeatureToggles
import com.tangem.features.biometry.impl.model.AskBiometryModel
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
    fun bindFeatureToggles(impl: DefaultBiometryFeatureToggles): BiometryFeatureToggles

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