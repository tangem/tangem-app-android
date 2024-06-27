package com.tangem.core.featuretoggle.di

import android.content.Context
import com.tangem.core.featuretoggle.manager.DevFeatureTogglesManager
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.core.featuretoggle.manager.ProdFeatureTogglesManager
import com.tangem.core.featuretoggle.storage.LocalFeatureTogglesStorage
import com.tangem.core.featuretoggle.version.DefaultVersionProvider
import com.tangem.core.featuretoggles.BuildConfig
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
        val localFeatureTogglesStorage = LocalFeatureTogglesStorage(assetLoader)
        val versionProvider = DefaultVersionProvider(context)

        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DevFeatureTogglesManager(
                localFeatureTogglesStorage = localFeatureTogglesStorage,
                appPreferencesStore = appPreferencesStore,
                versionProvider = versionProvider,
            )
        } else {
            ProdFeatureTogglesManager(
                localFeatureTogglesStorage = localFeatureTogglesStorage,
                versionProvider = versionProvider,
            )
        }
    }
}