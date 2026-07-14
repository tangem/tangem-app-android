package com.tangem.features.send.subcomponents.amount.di

import com.tangem.features.send.api.subcomponents.amount.*
import com.tangem.features.send.subcomponents.amount.DefaultSendAmountBlockComponent
import com.tangem.features.send.subcomponents.amount.DefaultSendAmountComponent
import com.tangem.features.send.subcomponents.amount.DefaultSendAmountReduceTrigger
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
    fun provideSendAmountComponentFactory(impl: DefaultSendAmountComponent.Factory): SendAmountComponent.Factory

    @Singleton
    @Binds
    fun provideSendAmountBlockComponentFactory(
        impl: DefaultSendAmountBlockComponent.Factory,
    ): SendAmountBlockComponent.Factory

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