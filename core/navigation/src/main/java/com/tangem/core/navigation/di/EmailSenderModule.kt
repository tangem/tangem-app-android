package com.tangem.core.navigation.di

import android.content.Context
import com.tangem.core.navigation.email.AndroidEmailSender
import com.tangem.core.navigation.email.EmailSender
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object EmailSenderModule {

    @Provides
    @ActivityScoped
    fun provideEmailSender(@ActivityContext context: Context): EmailSender {
        return AndroidEmailSender(context)
    }
}