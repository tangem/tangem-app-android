package com.tangem.core.abtests.di

import android.app.Application
import com.tangem.core.abtests.ABTestsConfig
import com.tangem.core.abtests.BuildConfig
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.abtests.manager.impl.AmplitudeABTestsManager
import com.tangem.core.abtests.manager.impl.StubABTestsManager
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ABTestsManagerModule {

    @Provides
    @Singleton
    fun provideABTestsManager(
        application: Application,
        abTestsConfig: ABTestsConfig,
        appScope: AppCoroutineScope,
    ): ABTestsManager {
        return if (BuildConfig.AB_TESTS_ENABLED) {
            AmplitudeABTestsManager(
                application = application,
                apiKey = if (BuildConfig.TESTER_MENU_ENABLED) {
                    requireNotNull(abTestsConfig.amplitudeApiKeyDev) {
                        "Amplitude api key not found in ${BuildConfig.BUILD_TYPE}"
                    }
                } else {
                    abTestsConfig.amplitudeApiKey
                },
                scope = appScope,
            )
        } else {
            StubABTestsManager()
        }
    }
}