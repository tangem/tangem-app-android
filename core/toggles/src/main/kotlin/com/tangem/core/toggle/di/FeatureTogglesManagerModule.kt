package com.tangem.core.toggle.di

import android.content.Context
import com.tangem.core.toggle.feature.FeatureTogglesManager
import com.tangem.core.toggle.feature.impl.DevFeatureTogglesManager
import com.tangem.core.toggle.feature.impl.ProdFeatureTogglesManager
import com.tangem.core.toggle.storage.LocalTogglesStorage
import com.tangem.core.toggle.version.DefaultVersionProvider
import com.tangem.core.toggles.BuildConfig
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
