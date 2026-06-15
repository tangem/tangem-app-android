package com.tangem.features.send.di

import com.tangem.features.send.DefaultSendFeatureToggles
import com.tangem.features.send.api.NFTSendComponent
import com.tangem.features.send.api.SendComponent
import com.tangem.features.send.api.SendEntryPointComponent
import com.tangem.features.send.api.SendFeatureToggles
import com.tangem.features.send.api.SendNotificationsComponent
import com.tangem.features.send.entrypoint.DefaultSendEntryPointComponent
import com.tangem.features.send.send.DefaultSendComponent
import com.tangem.features.send.sendnft.DefaultNFTSendComponent
import com.tangem.features.send.subcomponents.notifications.DefaultSendNotificationsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

internal object SendFeatureModule

@Module
@InstallIn(SingletonComponent::class)
internal interface SendFeatureModuleBinds {

    @Binds
    @Singleton
    fun bindSendFeatureToggles(impl: DefaultSendFeatureToggles): SendFeatureToggles

    @Binds
    @Singleton
    fun provideSendComponentFactory(impl: DefaultSendComponent.Factory): SendComponent.Factory

    @Binds
    @Singleton
    fun provideNFTSendComponentFactory(impl: DefaultNFTSendComponent.Factory): NFTSendComponent.Factory

    @Binds
    @Singleton
    fun provideNotificationComponentFactory(
        impl: DefaultSendNotificationsComponent.Factory,
    ): SendNotificationsComponent.Factory

    @Binds
    @Singleton
    fun provideSendEntryPointComponentFactory(
        impl: DefaultSendEntryPointComponent.Factory,
    ): SendEntryPointComponent.Factory
}