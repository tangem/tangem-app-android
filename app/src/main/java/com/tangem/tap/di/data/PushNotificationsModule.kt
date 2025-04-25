package com.tangem.tap.di.data

import com.tangem.tap.data.FirebasePushNotificationsTokenProvider
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PushNotificationsModule {

    @Binds
    @Singleton
    fun bindPushNotificationsTokenProvider(impl: FirebasePushNotificationsTokenProvider): PushNotificationsTokenProvider
}