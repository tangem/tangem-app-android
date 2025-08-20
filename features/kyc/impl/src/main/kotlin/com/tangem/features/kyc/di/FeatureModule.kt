package com.tangem.features.kyc.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.kyc.DefaultKycComponent
import com.tangem.features.kyc.DefaultKycModel
import com.tangem.features.kyc.KycComponent
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
    fun bindComponentFactory(impl: DefaultKycComponent.Factory): KycComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {
    @Binds
    @IntoMap
    @ClassKey(DefaultKycModel::class)
    fun provideModel(model: DefaultKycModel): Model
}