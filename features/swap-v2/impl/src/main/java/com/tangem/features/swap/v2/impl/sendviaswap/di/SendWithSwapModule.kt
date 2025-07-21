package com.tangem.features.swap.v2.impl.sendviaswap.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import com.tangem.features.swap.v2.impl.sendviaswap.DefaultSendWithSwapComponent
import com.tangem.features.swap.v2.impl.sendviaswap.model.SendWithSwapModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(ModelComponent::class)
internal interface SendWithSwapModule {

    @Binds
    @IntoMap
    @ClassKey(SendWithSwapModel::class)
    fun provideSendWithSwapModel(impl: SendWithSwapModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SendWithSwapModuleBinds {
    @Binds
    @Singleton
    fun provideSendWithSwapComponentFactory(impl: DefaultSendWithSwapComponent.Factory): SendWithSwapComponent.Factory
}