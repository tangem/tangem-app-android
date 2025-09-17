package com.tangem.tap.di

import com.tangem.tap.HuaweiPushNotificationsTokenProvider
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface HuaweiPushModule {

    @Binds
    @Singleton
    fun bindPushNotificationsTokenProvider(impl: HuaweiPushNotificationsTokenProvider): PushNotificationsTokenProvider
}