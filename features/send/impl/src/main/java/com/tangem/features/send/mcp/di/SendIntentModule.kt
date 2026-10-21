package com.tangem.features.send.mcp.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.api.mcp.SendIntentComponent
import com.tangem.features.send.mcp.DefaultSendIntentComponent
import com.tangem.features.send.mcp.model.SendIntentModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SendIntentComponentModule {

    @Binds
    @Singleton
    fun bindSendIntentComponentFactory(impl: DefaultSendIntentComponent.Factory): SendIntentComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface SendIntentModelModule {

    @Binds
    @IntoMap
    @ClassKey(SendIntentModel::class)
    fun provideSendIntentModel(model: SendIntentModel): Model
}