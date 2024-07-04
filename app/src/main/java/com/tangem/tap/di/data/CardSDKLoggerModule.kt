package com.tangem.tap.di.data

import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.TangemSdkLogger
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.tap.common.log.TangemCardSDKLogger
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardSDKLoggerModule {

    @Provides
    @Singleton
    fun provideCardSDKLogger(
        settingsRepository: SettingsRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): TangemSdkLogger {
        val logLevels = listOf(
            Log.Level.ApduCommand,
            Log.Level.Apdu,
            Log.Level.Tlv,
            Log.Level.Nfc,
            Log.Level.Command,
            Log.Level.Session,
            Log.Level.View,
            Log.Level.Network,
            Log.Level.Error,
            Log.Level.Biometric,
            Log.Level.Info,
        )

        return TangemCardSDKLogger(
            levels = logLevels,
            messageFormatter = LogFormat.StairsFormatter(),
            settingsRepository = settingsRepository,
            dispatchers = dispatchers,
        )
    }
}