package com.tangem.tap.di.core.navigation.email

import com.tangem.core.navigation.email.EmailSender
import com.tangem.tap.core.navigation.email.AndroidEmailSender
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object EmailSenderModule {

    @Provides
    @Singleton
    fun provideEmailSender(): EmailSender = AndroidEmailSender()
}