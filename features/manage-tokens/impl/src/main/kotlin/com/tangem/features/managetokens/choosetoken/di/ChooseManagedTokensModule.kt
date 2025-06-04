package com.tangem.features.managetokens.choosetoken.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.managetokens.choosetoken.DefaultChooseManagedTokensComponent
import com.tangem.features.managetokens.choosetoken.model.ChooseManagedTokensModel
import com.tangem.features.managetokens.component.ChooseManagedTokensComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@InstallIn(ModelComponent::class)
@Module
internal interface ChooseManagedTokensModule {

    @Binds
    @IntoMap
    @ClassKey(ChooseManagedTokensModel::class)
    fun provideChooseManagedTokensModel(impl: ChooseManagedTokensModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal interface ChooseManagedTokensModuleBinds {

    @Binds
    @Singleton
    fun provideChooseManagedTokensComponent(
        impl: DefaultChooseManagedTokensComponent.Factory,
    ): ChooseManagedTokensComponent.Factory
}