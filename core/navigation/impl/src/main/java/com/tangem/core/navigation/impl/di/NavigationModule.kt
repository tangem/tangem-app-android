package com.tangem.core.navigation.impl.di

import android.content.Context
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.core.navigation.email.EmailSender
import com.tangem.core.navigation.impl.email.AndroidEmailSender
import com.tangem.core.navigation.impl.email.EmailMessageTruncator
import com.tangem.core.navigation.impl.notifications.DefaultSystemNotificationsStateProvider
import com.tangem.core.navigation.impl.settings.IntentSettingsManager
import com.tangem.core.navigation.impl.share.IntentShareManager
import com.tangem.core.navigation.impl.url.DefaultAppStoreOpener
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.AppStoreOpener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NavigationModule {

    @Binds
    @Singleton
    fun bindAppStoreOpener(impl: DefaultAppStoreOpener): AppStoreOpener

    @Binds
    fun bindShareManager(impl: IntentShareManager): ShareManager

    @Binds
    @Singleton
    fun bindSystemNotificationsStateProvider(
        impl: DefaultSystemNotificationsStateProvider,
    ): SystemNotificationsStateProvider

    companion object {

        @Provides
        @Singleton
        fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager =
            IntentSettingsManager(context)

        @Provides
        @Singleton
        fun provideEmailSender(foregroundActivityProvider: ForegroundActivityProvider): EmailSender =
            AndroidEmailSender(
                messageTruncator = EmailMessageTruncator(),
                foregroundActivityProvider = foregroundActivityProvider,
            )
    }
}