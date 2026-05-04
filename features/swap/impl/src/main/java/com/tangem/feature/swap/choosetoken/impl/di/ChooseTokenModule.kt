package com.tangem.feature.swap.choosetoken.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridge
import com.tangem.feature.swap.choosetoken.api.ChooseTokenComponent
import com.tangem.feature.swap.choosetoken.impl.DefaultChooseTokenBridge
import com.tangem.feature.swap.choosetoken.impl.DefaultChooseTokenComponent
import com.tangem.feature.swap.choosetoken.impl.model.ChooseTokenModel
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