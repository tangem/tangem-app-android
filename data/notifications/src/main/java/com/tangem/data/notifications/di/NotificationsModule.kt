package com.tangem.data.notifications.di

import com.tangem.data.notifications.DefaultNotificationsRepository
import com.tangem.domain.notifications.repository.NotificationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NotificationsModule {

    @Binds
    @Singleton
    fun bindNotificationsRepository(repository: DefaultNotificationsRepository): NotificationsRepository
}