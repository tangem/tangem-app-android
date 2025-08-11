package com.tangem.tap.di.data

import com.tangem.tap.data.PushNotificationsTokenProviderImpl
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PushNotificationsTokenProviderModule {

    @Binds
    @Singleton
    fun bindPushNotificationsTokenProvider(impl: PushNotificationsTokenProviderImpl): PushNotificationsTokenProvider
}