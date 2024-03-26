package com.tangem.tap.domain.walletconnect2.di

import android.app.Application
import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.files.FileReader
import com.tangem.tap.domain.walletconnect.WalletConnectSdkHelper
import com.tangem.tap.domain.walletconnect2.app.TangemWcBlockchainHelper
import com.tangem.tap.domain.walletconnect2.app.WalletConnectEventsHandlerImpl
import com.tangem.tap.domain.walletconnect2.data.DefaultWalletConnectRepository
import com.tangem.tap.domain.walletconnect2.data.DefaultWalletConnectSessionsRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.domain.walletconnect2.domain.WcJrpcRequestsDeserializer
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
internal object WalletConnectInteractorModule {

    @Provides
    @ActivityScoped
    fun provideWalletConnectInteractor(
        wcRepository: WalletConnectRepository,
        wcSessionsRepository: WalletConnectSessionsRepository,
    ): WalletConnectInteractor {
        return WalletConnectInteractor(
            handler = WalletConnectEventsHandlerImpl(),
            walletConnectRepository = wcRepository,
            sessionsRepository = wcSessionsRepository,
            sdkHelper = WalletConnectSdkHelper(),
            blockchainHelper = TangemWcBlockchainHelper(),
            dispatcher = AppCoroutineDispatcherProvider(),
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object WalletConnectModule {

    @Provides
    @Singleton
    fun bindWalletConnectRepository(
        application: Application,
        wcRequestDeserializer: WcJrpcRequestsDeserializer,
        analyticsHandler: AnalyticsEventHandler,
    ): WalletConnectRepository {
        return DefaultWalletConnectRepository(
            application = application,
            wcRequestDeserializer = wcRequestDeserializer,
            analyticsHandler = analyticsHandler,
        )
    }

    @Provides
    @Singleton
    fun bindWalletConnectSessionsRepository(
        @SdkMoshi moshi: Moshi,
        fileReader: FileReader,
    ): WalletConnectSessionsRepository {
        return DefaultWalletConnectSessionsRepository(
            moshi = moshi,
            fileReader = fileReader,
        )
    }
}
