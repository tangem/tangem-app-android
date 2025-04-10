package com.tangem.features.send.v2.subcomponents.fee.di

import com.tangem.features.send.v2.subcomponents.fee.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal interface SendFeeModule {

    @Binds
    @Singleton
    fun provideSendFeeReloadTrigger(impl: DefaultSendFeeReloadTrigger): SendFeeReloadTrigger

    @Binds
    @Singleton
    fun provideSendFeeReloadListener(impl: DefaultSendFeeReloadTrigger): SendFeeReloadListener

    @Binds
    @Singleton
    fun provideSendFeeCheckReloadTrigger(impl: DefaultSendFeeReloadTrigger): SendFeeCheckReloadTrigger

    @Binds
    @Singleton
    fun provideSendFeeCheckReloadListener(impl: DefaultSendFeeReloadTrigger): SendFeeCheckReloadListener
}