package com.tangem.tap.di

import android.content.Context
import com.tangem.tap.common.deeplink.DefaultDeeplinkLauncher
import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.tap.common.finisher.AndroidAppFinisher
import com.tangem.tap.common.settings.IntentSettingsManager
import com.tangem.tap.common.share.IntentShareManager
import com.tangem.tap.common.url.CustomTabsUrlOpener
import com.tangem.tap.core.DefaultAppCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface UtilsModule {

    @Binds
    fun provideAppScope(defaultAppScope: DefaultAppCoroutineScope): AppCoroutineScope

    companion object {

        @Provides
        @Singleton
        fun provideShareManager(): ShareManager = IntentShareManager()

        @Provides
        @Singleton
        fun provideUrlOpener(): UrlOpener = CustomTabsUrlOpener()

        @Provides
        @Singleton
        fun provideAppFinisher(@ApplicationContext context: Context): AppFinisher = AndroidAppFinisher(context)

        @Provides
        @Singleton
        fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager =
            IntentSettingsManager(context)

        @Provides
        @Singleton
        fun provideDeeplinkLauncher(@ApplicationContext context: Context, urlOpener: UrlOpener): DeeplinkLauncher =
            DefaultDeeplinkLauncher(context, urlOpener)
    }
}