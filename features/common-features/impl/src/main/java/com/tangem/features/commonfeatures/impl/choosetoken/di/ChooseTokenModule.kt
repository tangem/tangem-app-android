package com.tangem.features.commonfeatures.impl.choosetoken.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.commonfeatures.impl.choosetoken.DefaultChooseTokenBridge
import com.tangem.features.commonfeatures.impl.choosetoken.DefaultChooseTokenComponent
import com.tangem.features.commonfeatures.impl.choosetoken.model.ChooseTokenModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(ModelComponent::class)
internal interface ChooseTokenModelModule {

    @Binds
    @IntoMap
    @ClassKey(ChooseTokenModel::class)
    fun provideChooseTokenModel(model: ChooseTokenModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal interface ChooseTokenFeatureModule {

    @Binds
    @Singleton
    fun provideChooseTokenComponentFactory(impl: DefaultChooseTokenComponent.Factory): ChooseTokenComponent.Factory

    @Binds
    @Singleton
    fun provideDefaultChooseTokenBridgeFactory(impl: DefaultChooseTokenBridge.Factory): ChooseTokenBridge.Factory
}