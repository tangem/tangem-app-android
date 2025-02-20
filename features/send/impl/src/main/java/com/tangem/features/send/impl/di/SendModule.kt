package com.tangem.features.send.impl.di

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.api.SendComponent
import com.tangem.features.send.impl.DefaultSendComponent
import com.tangem.features.send.impl.navigation.DefaultSendRouter
import com.tangem.features.send.impl.navigation.InnerSendRouter
import com.tangem.features.send.impl.presentation.model.SendModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface SendModule {

    @Binds
    fun bindComponentFactory(factory: DefaultSendComponent.Factory): SendComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(SendModel::class)
    fun bindModel(model: SendModel): Model
}

@Module
@InstallIn(DecomposeComponent::class)
internal interface SendModelModule {

    @Binds
    @ComponentScoped
    fun bindRouter(router: DefaultSendRouter): InnerSendRouter
}