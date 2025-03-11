package com.tangem.core.configtoggle.di

import android.content.Context
import com.tangem.core.configtoggle.BuildConfig
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.feature.impl.DevFeatureTogglesManager
import com.tangem.core.configtoggle.feature.impl.ProdFeatureTogglesManager
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
internal object FeatureTogglesManagerModule {

    @Provides
    @Singleton
    fun provideFeatureTogglesManager(
        @ApplicationContext context: Context,
        assetLoader: AssetLoader,
        appPreferencesStore: AppPreferencesStore,
    ): FeatureTogglesManager {
        val localTogglesStorage = LocalTogglesStorage(assetLoader)
        val versionProvider = DefaultVersionProvider(context)

        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DevFeatureTogglesManager(
                localTogglesStorage = localTogglesStorage,
                appPreferencesStore = appPreferencesStore,
                versionProvider = versionProvider,
            )
        } else {
            ProdFeatureTogglesManager(
                localTogglesStorage = localTogglesStorage,
                versionProvider = versionProvider,
            )
        }
    }
}