package com.tangem.features.send.v2.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.send.v2.DefaultSendFeatureToggles
import com.tangem.features.send.v2.api.NFTSendComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.send.DefaultSendComponent
import com.tangem.features.send.v2.sendnft.DefaultNFTSendComponent
import com.tangem.features.send.v2.subcomponents.notifications.DefaultSendNotificationsComponent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object SendFeatureModule {

    @Provides
    fun provideSendFeatureToggles(featureTogglesManager: FeatureTogglesManager): SendFeatureToggles {
        return DefaultSendFeatureToggles(featureTogglesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SendFeatureModuleBinds {

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
}