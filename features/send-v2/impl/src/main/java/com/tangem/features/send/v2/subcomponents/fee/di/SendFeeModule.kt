package com.tangem.features.send.v2.subcomponents.fee.di

import com.tangem.features.send.v2.subcomponents.fee.DefaultSendFeeReloadTrigger
import com.tangem.features.send.v2.subcomponents.fee.SendFeeCheckReloadTrigger
import com.tangem.features.send.v2.subcomponents.fee.SendFeeReloadTrigger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object SendFeeModule {

    @Provides
    @Singleton
    fun provideSendFeeReloadTrigger(): SendFeeReloadTrigger {
        return DefaultSendFeeReloadTrigger()
    }

    @Provides
    @Singleton
    fun provideSendFeeCheckReloadTrigger(): SendFeeCheckReloadTrigger {
        return DefaultSendFeeReloadTrigger()
    }
}