package com.tangem.features.send.v2.subcomponents.amount.di

import com.tangem.features.send.v2.subcomponents.amount.DefaultSendAmountReduceTrigger
import com.tangem.features.send.v2.subcomponents.amount.SendAmountReduceListener
import com.tangem.features.send.v2.subcomponents.amount.SendAmountReduceTrigger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object SendAmountModule {

    @Provides
    @Singleton
    fun provideSendAmountReduceTrigger(): SendAmountReduceTrigger {
        return DefaultSendAmountReduceTrigger()
    }

    @Provides
    @Singleton
    fun provideSendAmountReduceListener(): SendAmountReduceListener {
        return DefaultSendAmountReduceTrigger()
    }
}