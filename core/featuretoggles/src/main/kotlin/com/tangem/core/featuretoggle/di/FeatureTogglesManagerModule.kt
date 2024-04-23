package com.tangem.core.featuretoggle.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.core.featuretoggle.manager.DevFeatureTogglesManager
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.core.featuretoggle.manager.ProdFeatureTogglesManager
import com.tangem.core.featuretoggle.storage.LocalFeatureTogglesStorage
import com.tangem.core.featuretoggle.version.DefaultVersionProvider
import com.tangem.core.featuretoggles.BuildConfig
import com.tangem.datasource.asset.reader.AssetReader
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
    @OptIn(ExperimentalStdlibApi::class)
    fun provideFeatureTogglesManager(
        @ApplicationContext context: Context,
        assetReader: AssetReader,
        appPreferencesStore: AppPreferencesStore,
    ): FeatureTogglesManager {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val localFeatureTogglesStorage = LocalFeatureTogglesStorage(
            assetReader = assetReader,
            jsonAdapter = moshi.adapter(),
        )
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
