package com.tangem.tap.domain.walletconnect2.di

import android.app.Application
import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.files.FileReader
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.legacy.WalletConnectSessionsRepository
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.walletconnect.components.WalletConnectFeatureToggles
import com.tangem.tap.domain.walletconnect.WalletConnectSdkHelper
import com.tangem.tap.domain.walletconnect2.app.TangemWcBlockchainHelper
import com.tangem.tap.domain.walletconnect2.app.WalletConnectEventsHandlerImpl
import com.tangem.tap.domain.walletconnect2.data.DefaultLegacyWalletConnectRepository
import com.tangem.tap.domain.walletconnect2.data.DefaultLegacyWalletConnectRepositoryFacade
import com.tangem.tap.domain.walletconnect2.data.DefaultWalletConnectSessionsRepository
import com.tangem.tap.domain.walletconnect2.data.LegacyWalletConnectRepositoryStub
import com.tangem.tap.domain.walletconnect2.domain.LegacyWalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WcJrpcRequestsDeserializer
import com.tangem.tap.domain.walletconnect2.toggles.DefaultWalletConnectFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletConnectInteractorModule {

    @Provides
    @Singleton
    fun provideWalletConnectInteractor(
        wcRepository: LegacyWalletConnectRepository,
        wcSessionsRepository: WalletConnectSessionsRepository,
        currenciesRepository: CurrenciesRepository,
        walletManagersFacade: WalletManagersFacade,
        userWalletsListManager: UserWalletsListManager,
        walletConnectFeatureToggles: WalletConnectFeatureToggles,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): WalletConnectInteractor {
        return WalletConnectInteractor(
            handler = WalletConnectEventsHandlerImpl(),
            walletConnectRepository = wcRepository,
            sessionsRepository = wcSessionsRepository,
            sdkHelper = WalletConnectSdkHelper(),
            blockchainHelper = TangemWcBlockchainHelper(),
            currenciesRepository = currenciesRepository,
            walletManagersFacade = walletManagersFacade,
            userWalletsListManager = userWalletsListManager,
            dispatchers = coroutineDispatcherProvider,
            walletConnectFeatureToggles = walletConnectFeatureToggles,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object WalletConnectModule {

    @Provides
    @Singleton
    fun provideWalletConnectFeatureToggles(featureTogglesManager: FeatureTogglesManager): WalletConnectFeatureToggles {
        return DefaultWalletConnectFeatureToggles(featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideWalletConnectRepository(
        application: Application,
        wcRequestDeserializer: WcJrpcRequestsDeserializer,
        analyticsHandler: AnalyticsEventHandler,
        walletConnectFeatureToggles: WalletConnectFeatureToggles,
        wcInitializeUseCase: WcInitializeUseCase,
        wcPairService: WcPairService,
    ): LegacyWalletConnectRepository {
        val legacy = DefaultLegacyWalletConnectRepository(
            application = application,
            wcRequestDeserializer = wcRequestDeserializer,
            analyticsHandler = analyticsHandler,
        )
        val stub = LegacyWalletConnectRepositoryStub()
        return DefaultLegacyWalletConnectRepositoryFacade(
            stub,
            legacy,
            walletConnectFeatureToggles,
            wcInitializeUseCase,
            wcPairService,
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