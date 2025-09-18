package com.tangem.core.configtoggle.di

import android.content.Context
import com.tangem.core.configtoggle.BuildConfig
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.core.configtoggle.blockchain.impl.DevExcludedBlockchainsManager
import com.tangem.core.configtoggle.blockchain.impl.ProdExcludedBlockchainsManager
import com.tangem.core.configtoggle.storage.LocalTogglesStorage
import com.tangem.core.configtoggle.version.DefaultVersionProvider
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
        appPreferencesStore: AppPreferencesStore,
    ): ExcludedBlockchainsManager {
        val versionProvider = DefaultVersionProvider(context)

        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DevExcludedBlockchainsManager(
                versionProvider = versionProvider,
                localTogglesStorage = LocalTogglesStorage(
                    appPreferencesStore = appPreferencesStore,
                    preferencesKey = LocalTogglesStorage.EXCLUDED_BLOCKCHAINS_KEY,
                ),
            )
        } else {
            ProdExcludedBlockchainsManager(versionProvider = versionProvider)
        }
    }
}