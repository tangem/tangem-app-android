package com.tangem.features.send.v2.subcomponents.amount.di

import com.tangem.features.send.v2.subcomponents.amount.*
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