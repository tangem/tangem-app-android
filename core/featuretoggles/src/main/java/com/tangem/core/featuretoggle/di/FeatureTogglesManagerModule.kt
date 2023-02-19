package com.tangem.core.featuretoggle.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.core.featuretoggle.manager.DevFeatureTogglesManager
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.core.featuretoggle.manager.ProdFeatureTogglesManager
import com.tangem.core.featuretoggle.storage.LocalFeatureTogglesStorage
import com.tangem.core.featuretoggles.BuildConfig
import com.tangem.datasource.local.AppPreferenceStorage
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
        appPreferenceStorage: AppPreferenceStorage,
    ): FeatureTogglesManager {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val localFeatureTogglesStorage = LocalFeatureTogglesStorage(context = context, jsonAdapter = moshi.adapter())

        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DevFeatureTogglesManager(
                localFeatureTogglesStorage = localFeatureTogglesStorage,
                appPreferenceStorage = appPreferenceStorage,
                jsonAdapter = moshi.adapter(),
                context = context,
            )
        } else {
            ProdFeatureTogglesManager(
                localFeatureTogglesStorage = localFeatureTogglesStorage,
                context = context,
            )
        }
    }
}
