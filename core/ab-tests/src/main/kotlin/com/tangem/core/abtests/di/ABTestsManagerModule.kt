package com.tangem.core.abtests.di

import android.app.Application
import com.tangem.core.abtests.BuildConfig
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.abtests.manager.impl.AmplitudeABTestsManager
import com.tangem.core.abtests.manager.impl.StubABTestsManager
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ABTestsManagerModule {

    @Provides
    @Singleton
    fun provideABTestsManager(
        application: Application,
        environmentConfigStorage: EnvironmentConfigStorage,
        dispatchers: CoroutineDispatcherProvider,
    ): ABTestsManager {
        return if (BuildConfig.AB_TESTS_ENABLED) {
            StubABTestsManager()
        } else {
            AmplitudeABTestsManager(
                application = application,
                apiKeyProvider = Provider { environmentConfigStorage.getConfigSync().amplitudeApiKey },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            )
        }
    }
}