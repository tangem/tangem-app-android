package com.tangem.core.analytics.handlers.firebase

import android.app.Application
import android.content.pm.ApplicationInfo
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.impl.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FirebaseModule {

    /**
     * A library BuildConfig.DEBUG is false in the mocked build, unlike the app one, so the debuggable flag of the
     * installed app decides whether events go to Firebase.
     */
    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideFirebaseHandler(application: Application): Set<AnalyticsHandler> {
        val isDebuggable = application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val client = when {
            !isDebuggable -> FirebaseClient()
            BuildConfig.LOG_ENABLED -> FirebaseLogClient()
            else -> return emptySet()
        }

        return setOf(FirebaseAnalyticsHandler(client))
    }

    @Provides
    @Singleton
    fun provideAppInstanceIdProvider(): AppInstanceIdProvider = FirebaseAppInstanceIdProvider()
}