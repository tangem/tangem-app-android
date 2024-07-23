package com.tangem.tap.di

import android.content.Context
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.feedback.FeedbackManager
import com.tangem.tap.common.feedback.ProxyFeedbackManager
import com.tangem.tap.common.finisher.AndroidAppFinisher
import com.tangem.tap.common.settings.IntentSettingsManager
import com.tangem.tap.common.share.IntentShareManager
import com.tangem.tap.common.url.CustomTabsUrlOpener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UtilsModule {

    @Provides
    @Singleton
    fun provideShareManager(): ShareManager = IntentShareManager()

    @Provides
    @Singleton
    fun provideUrlOpener(): UrlOpener = CustomTabsUrlOpener()

    @Provides
    @Singleton
    fun provideFeedbackManager(): FeedbackManager = ProxyFeedbackManager()

    @Provides
    @Singleton
    fun provideAppFinisher(@ApplicationContext context: Context): AppFinisher = AndroidAppFinisher(context)

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager = IntentSettingsManager(context)
}