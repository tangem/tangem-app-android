package com.tangem.features.send.v2.subcomponents.amount.di

import com.tangem.features.send.v2.subcomponents.amount.*
import com.tangem.features.send.v2.subcomponents.amount.DefaultSendAmountReduceTrigger
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
    fun provideSendAmountUpdateQRListener(impl: DefaultSendAmountReduceTrigger): SendAmountUpdateQRListener

    @Singleton
    @Binds
    fun provideSendAmountUpdateQRTrigger(impl: DefaultSendAmountReduceTrigger): SendAmountUpdateQRTrigger
}