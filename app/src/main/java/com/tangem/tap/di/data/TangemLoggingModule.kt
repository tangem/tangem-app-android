package com.tangem.tap.di.data

import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.TangemSdkLogger
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.tap.common.log.TangemAppLoggerInitializer
import com.tangem.tap.common.log.TangemCardSDKLogger
import com.tangem.tap.data.TangemBlockchainSDKLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TangemLoggingModule {

    @Provides
    @Singleton
    fun provideAppLoggerInitializer(appLogsStore: AppLogsStore): TangemAppLoggerInitializer {
        return TangemAppLoggerInitializer(appLogsStore)
    }

    @Provides
    @Singleton
    fun provideCardSDKLogger(appLogsStore: AppLogsStore): TangemSdkLogger {
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
            appLogsStore = appLogsStore,
        )
    }

    @Provides
    @Singleton
    fun provideBlockchainSDKLogger(appLogsStore: AppLogsStore): BlockchainSDKLogger {
        return TangemBlockchainSDKLogger(appLogsStore)
    }
}