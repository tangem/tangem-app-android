package com.tangem.core.configtoggle.di

import android.content.Context
import com.tangem.core.configtoggle.BuildConfig
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.core.configtoggle.blockchain.MutableExcludedBlockchainsManager
import com.tangem.core.configtoggle.blockchain.impl.DefaultExcludedBlockchainsManager
import com.tangem.core.configtoggle.storage.LocalTogglesStorage
import com.tangem.core.configtoggle.version.DefaultVersionProvider
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.preferences.AppPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ExcludedBlockchainsManagerModule {

    @Provides
    @Singleton
    fun provideExcludedBlockchainsManager(
        @ApplicationContext context: Context,
        assetLoader: AssetLoader,
        appPreferencesStore: AppPreferencesStore,
    ): ExcludedBlockchainsManager {
        val localTogglesStorage = LocalTogglesStorage(assetLoader)
        val versionProvider = DefaultVersionProvider(context)

        return DefaultExcludedBlockchainsManager(
            localTogglesStorage,
            appPreferencesStore,
            versionProvider,
        )
    }

    @Provides
    @Singleton
    fun provideMutableExcludedBlockchainsManager(
        manager: ExcludedBlockchainsManager,
    ): MutableExcludedBlockchainsManager? {
        if (!BuildConfig.TESTER_MENU_ENABLED) return null

        return manager as MutableExcludedBlockchainsManager
    }
}