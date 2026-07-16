package com.tangem.features.send.subcomponents.destination.di

import com.tangem.features.send.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.subcomponents.destination.DefaultSendDestinationComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SendDestinationModule {
    @Singleton
    @Binds
    fun provideSendDestinationComponentFactory(
        impl: DefaultSendDestinationComponent.Factory,
    ): SendDestinationComponent.Factory

    @Singleton
    @Binds
    fun provideSendDestinationBlockComponentFactory(
        impl: DefaultSendDestinationBlockComponent.Factory,
    ): SendDestinationBlockComponent.Factory
}