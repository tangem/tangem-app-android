package com.tangem.data.walletconnect.di

import android.app.Application
import com.squareup.moshi.Moshi
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.walletconnect.DefaultWalletConnectRepository
import com.tangem.data.walletconnect.initialize.DefaultWcInitializeUseCase
import com.tangem.data.walletconnect.network.ethereum.WcEthNetwork
import com.tangem.data.walletconnect.network.solana.WcSolanaNetwork
import com.tangem.data.walletconnect.pair.*
import com.tangem.data.walletconnect.request.DefaultWcRequestService
import com.tangem.data.walletconnect.request.DefaultWcRequestUseCaseFactory
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter
import com.tangem.data.walletconnect.respond.DefaultWcRespondService
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sessions.DefaultWcSessionsManager
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletconnect.WalletConnectStore
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.WcRequestService
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.model.legacy.WalletConnectSessionsRepository
import com.tangem.domain.walletconnect.repository.WalletConnectRepository
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.disconnect.WcDisconnectUseCase
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.lib.crypto.UserWalletManager
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
internal object WalletConnectDataModule {

    @Provides
    @Singleton
    fun providesWalletConnectRepository(
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletConnectRepository {
        return DefaultWalletConnectRepository(userWalletsStore, dispatchers)
    }

    @Provides
    @Singleton
    fun wcInitializeUseCase(
        application: Application,
        sessionsManager: DefaultWcSessionsManager,
        networkService: DefaultWcRequestService,
        pairSdkDelegate: WcPairSdkDelegate,
    ): WcInitializeUseCase = DefaultWcInitializeUseCase(
        application = application,
        sessionsManager = sessionsManager,
        networkService = networkService,
        pairSdkDelegate = pairSdkDelegate,
    )

    @Provides
    @Singleton
    fun defaultWcPairUseCase(): WcPairService = DefaultWcPairService()

    @Provides
    @Singleton
    fun wcPairUseCaseFactory(default: DefaultWcPairUseCase.Factory): WcPairUseCase.Factory = default

    @Provides
    @Singleton
    fun sdkDelegate(): WcPairSdkDelegate = WcPairSdkDelegate()

    @Provides
    @Singleton
    fun defaultWcSessionsManager(
        store: WalletConnectStore,
        dispatchers: CoroutineDispatcherProvider,
        legacyStore: WalletConnectSessionsRepository,
        getWallets: GetWalletsUseCase,
    ): DefaultWcSessionsManager {
        val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
        return DefaultWcSessionsManager(
            store = store,
            dispatchers = dispatchers,
            legacyStore = legacyStore,
            getWallets = getWallets,
            scope = scope,
        )
    }

    @Provides
    @Singleton
    fun wcSessionsManager(default: DefaultWcSessionsManager): WcSessionsManager = default

    @Provides
    @Singleton
    fun wcRequestService(default: DefaultWcRequestService): WcRequestService = default

    @Provides
    @Singleton
    fun defaultWcRequestService(diHelperBox: DiHelperBox): DefaultWcRequestService {
        return DefaultWcRequestService(
            requestConverters = diHelperBox.handlers,
        )
    }

    @Provides
    @Singleton
    fun wcRespondService(): WcRespondService = DefaultWcRespondService()

    @Provides
    @Singleton
    fun wcEthNetwork(
        @SdkMoshi moshi: Moshi,
        excludedBlockchains: ExcludedBlockchains,
        sessionsManager: WcSessionsManager,
        factories: WcEthNetwork.Factories,
    ): WcEthNetwork = WcEthNetwork(
        moshi = moshi,
        excludedBlockchains = excludedBlockchains,
        sessionsManager = sessionsManager,
        factories = factories,
    )

    @Provides
    @Singleton
    fun wcSolanaNetwork(
        @SdkMoshi moshi: Moshi,
        excludedBlockchains: ExcludedBlockchains,
        sessionsManager: WcSessionsManager,
        factories: WcSolanaNetwork.Factories,
        walletManager: UserWalletManager,
    ): WcSolanaNetwork = WcSolanaNetwork(
        moshi = moshi,
        sessionsManager = sessionsManager,
        factories = factories,
        excludedBlockchains = excludedBlockchains,
        walletManager = walletManager,
    )

    @Provides
    @Singleton
    fun caipNamespaceDelegate(
        diHelperBox: DiHelperBox,
        walletManagersFacade: WalletManagersFacade,
    ): CaipNamespaceDelegate = CaipNamespaceDelegate(
        namespaceConverters = diHelperBox.converters,
        walletManagersFacade = walletManagersFacade,
    )

    @Provides
    @Singleton
    fun associateNetworksDelegate(
        diHelperBox: DiHelperBox,
        getWallets: GetWalletsUseCase,
        currenciesRepository: CurrenciesRepository,
    ): AssociateNetworksDelegate = AssociateNetworksDelegate(
        namespaceConverters = diHelperBox.converters,
        getWallets = getWallets,
        currenciesRepository = currenciesRepository,
    )

    @Provides
    @Singleton
    fun diHelperBox(ethNetwork: WcEthNetwork, solanaNetwork: WcSolanaNetwork) = DiHelperBox(
        handlers = setOf(
            ethNetwork,
            solanaNetwork,
        ),
        converters = setOf(
            ethNetwork,
            solanaNetwork,
        ),
    )

    @Provides
    @Singleton
    fun wcRequestUseCaseFactory(diHelperBox: DiHelperBox): WcRequestUseCaseFactory {
        return DefaultWcRequestUseCaseFactory(diHelperBox.handlers)
    }

    @Provides
    @Singleton
    fun providesWcDisconnectUseCase(sessionsManager: WcSessionsManager): WcDisconnectUseCase {
        return WcDisconnectUseCase(sessionsManager)
    }

    internal class DiHelperBox(
        val converters: Set<WcNamespaceConverter>,
        val handlers: Set<WcRequestToUseCaseConverter>,
    )
}