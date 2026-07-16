package com.tangem.features.send.subcomponents.amount.di

import com.tangem.features.send.subcomponents.amount.DefaultSendAmountReduceTrigger
import com.tangem.features.send.subcomponents.amount.SendAmountReduceListener
import com.tangem.features.send.subcomponents.amount.SendAmountReduceTrigger
import com.tangem.features.send.subcomponents.amount.SendAmountUpdateListener
import com.tangem.features.send.subcomponents.amount.SendAmountUpdateTrigger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal interface SendAmountModule {

    @Singleton
    @Binds
    fun provideSendAmountReduceTrigger(impl: DefaultSendAmountReduceTrigger): SendAmountReduceTrigger

    @Singleton
    @Binds
    fun provideSendAmountReduceListener(impl: DefaultSendAmountReduceTrigger): SendAmountReduceListener

    @Singleton
    @Binds
    fun provideSendAmountUpdateListener(impl: DefaultSendAmountReduceTrigger): SendAmountUpdateListener

    @Singleton
    @Binds
    fun provideSendAmountUpdateTrigger(impl: DefaultSendAmountReduceTrigger): SendAmountUpdateTrigger
}