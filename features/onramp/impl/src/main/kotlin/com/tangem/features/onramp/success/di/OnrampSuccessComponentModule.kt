package com.tangem.features.onramp.success.di

import com.tangem.features.onramp.component.OnrampSuccessComponent
import com.tangem.features.onramp.success.DefaultOnrampSuccessComponent
import com.tangem.features.onramp.success.DefaultOnrampSuccessScreenTrigger
import com.tangem.features.onramp.success.OnrampSuccessScreenListener
import com.tangem.features.onramp.success.OnrampSuccessScreenTrigger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampSuccessComponentModule {
    @Binds
    @Singleton
    fun bindOnrampSuccessComponentFactory(
        factory: DefaultOnrampSuccessComponent.Factory,
    ): OnrampSuccessComponent.Factory

    @Binds
    @Singleton
    fun bindOnrampSuccessScreenTrigger(impl: DefaultOnrampSuccessScreenTrigger): OnrampSuccessScreenTrigger

    @Binds
    @Singleton
    fun bindOnrampSuccessScreenListener(impl: DefaultOnrampSuccessScreenTrigger): OnrampSuccessScreenListener
}