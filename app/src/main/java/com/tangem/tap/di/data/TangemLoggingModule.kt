package com.tangem.tap.di.data

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.tap.common.log.TangemBlockchainSDKLogger
import com.tangem.tap.common.log.TangemCardSDKLogger
import com.tangem.tap.common.log.TangemLoggingInitializer
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
    fun provideLoggingInitializer(appLogsStore: AppLogsStore): TangemLoggingInitializer {
        return TangemLoggingInitializer(
            appLogsStore = appLogsStore,
            tangemSdkLogger = TangemCardSDKLogger(appLogsStore),
        )
    }

    @Provides
    @Singleton
    fun provideBlockchainSDKLogger(appLogsStore: AppLogsStore): BlockchainSDKLogger {
        return TangemBlockchainSDKLogger(appLogsStore)
    }
}