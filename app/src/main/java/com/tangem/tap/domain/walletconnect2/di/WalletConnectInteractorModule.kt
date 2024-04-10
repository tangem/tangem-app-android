package com.tangem.tap.domain.walletconnect2.di

import android.app.Application
import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.files.FileReader
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.tap.domain.walletconnect.WalletConnectSdkHelper
import com.tangem.tap.domain.walletconnect2.app.TangemWcBlockchainHelper
import com.tangem.tap.domain.walletconnect2.app.WalletConnectEventsHandlerImpl
import com.tangem.tap.domain.walletconnect2.data.DefaultWalletConnectRepository
import com.tangem.tap.domain.walletconnect2.data.DefaultWalletConnectSessionsRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.domain.walletconnect2.domain.WcJrpcRequestsDeserializer
import com.tangem.tap.domain.walletconnect2.toggles.WalletConnectFeatureToggles
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
        walletConnectFeatureToggles: WalletConnectFeatureToggles,
        currenciesRepository: CurrenciesRepository,
        walletManagersFacade: WalletManagersFacade,
        walletsStateHolder: WalletsStateHolder,
    ): WalletConnectInteractor {
        return WalletConnectInteractor(
            handler = WalletConnectEventsHandlerImpl(),
            walletConnectRepository = wcRepository,
            sessionsRepository = wcSessionsRepository,
            sdkHelper = WalletConnectSdkHelper(),
            blockchainHelper = TangemWcBlockchainHelper(walletConnectFeatureToggles),
            currenciesRepository = currenciesRepository,
            walletManagersFacade = walletManagersFacade,
            walletsStateHolder = walletsStateHolder,
            dispatchers = AppCoroutineDispatcherProvider(),
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object WalletConnectModule {

    @Provides
    @Singleton
    fun provideWalletConnectFeatureToggles(featureTogglesManager: FeatureTogglesManager): WalletConnectFeatureToggles {
        return WalletConnectFeatureToggles(featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideWalletConnectRepository(
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
    fun provideWalletConnectSessionsRepository(
        @SdkMoshi moshi: Moshi,
        fileReader: FileReader,
    ): WalletConnectSessionsRepository {
        return DefaultWalletConnectSessionsRepository(
            moshi = moshi,
            fileReader = fileReader,
        )
    }
}
