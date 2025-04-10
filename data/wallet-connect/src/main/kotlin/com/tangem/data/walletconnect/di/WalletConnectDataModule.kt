package com.tangem.data.walletconnect.di

import android.app.Application
import com.squareup.moshi.Moshi
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.walletconnect.DefaultWalletConnectRepository
import com.tangem.data.walletconnect.initialize.DefaultWcInitializeUseCase
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.network.ethereum.WcEthNetwork
import com.tangem.data.walletconnect.network.solana.WcSolanaNetwork
import com.tangem.data.walletconnect.pair.AssociateNetworksDelegate
import com.tangem.data.walletconnect.pair.CaipNamespaceDelegate
import com.tangem.data.walletconnect.pair.DefaultWcPairUseCase
import com.tangem.data.walletconnect.pair.WcPairSdkDelegate
import com.tangem.data.walletconnect.request.DefaultWcRequestService
import com.tangem.data.walletconnect.request.WcMethodHandler
import com.tangem.data.walletconnect.respond.DefaultWcRespondService
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sessions.DefaultWcSessionsManager
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletconnect.WalletConnectStore
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.legacy.WalletConnectSessionsRepository
import com.tangem.domain.walletconnect.repository.WalletConnectRepository
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.request.WcRequestService
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
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
    fun defaultWcPairUseCase(
        sessionsManager: WcSessionsManager,
        associateNetworksDelegate: AssociateNetworksDelegate,
        caipNamespaceDelegate: CaipNamespaceDelegate,
        sdkDelegate: WcPairSdkDelegate,
    ): DefaultWcPairUseCase = DefaultWcPairUseCase(
        sessionsManager = sessionsManager,
        associateNetworksDelegate = associateNetworksDelegate,
        caipNamespaceDelegate = caipNamespaceDelegate,
        sdkDelegate = sdkDelegate,
    )

    @Provides
    @Singleton
    fun wcPairUseCase(default: DefaultWcPairUseCase): WcPairUseCase = default

    @Provides
    @Singleton
    fun sdkDelegate(): WcPairSdkDelegate = WcPairSdkDelegate()

    @Provides
    @Singleton
    fun defaultWcSessionsManager(
        store: WalletConnectStore,
        dispatchers: CoroutineDispatcherProvider,
        legacyStore: WalletConnectSessionsRepository,
        getUserWallet: GetUserWalletUseCase,
        getWallets: GetWalletsUseCase,
    ): DefaultWcSessionsManager {
        val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
        return DefaultWcSessionsManager(
            store = store,
            dispatchers = dispatchers,
            legacyStore = legacyStore,
            getWallets = getWallets,
            getUserWallet = getUserWallet,
            scope = scope,
        )
    }

    @Provides
    @Singleton
    fun wcSessionsManager(default: DefaultWcSessionsManager): WcSessionsManager = default

    @Provides
    @Singleton
    fun defaultWcRequestService(
        sessionsManager: WcSessionsManager,
        respondService: WcRespondService,
        dispatchers: CoroutineDispatcherProvider,
        diHelperBox: DiHelperBox,
    ): DefaultWcRequestService {
        val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
        return DefaultWcRequestService(
            sessionsManager = sessionsManager,
            respondService = respondService,
            requestAdapters = diHelperBox.handlers,
            scope = scope,
        )
    }

    @Provides
    @Singleton
    fun wcRequestService(default: DefaultWcRequestService): WcRequestService = default

    @Provides
    @Singleton
    fun wcRespondService(): WcRespondService = DefaultWcRespondService()

    @Provides
    @Singleton
    fun wcEthNetwork(@SdkMoshi moshi: Moshi): WcEthNetwork = WcEthNetwork(
        moshi = moshi,
    )

    @Provides
    @Singleton
    fun wcSolanaNetwork(): WcSolanaNetwork = WcSolanaNetwork()

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
        excludedBlockchains: ExcludedBlockchains,
    ): AssociateNetworksDelegate = AssociateNetworksDelegate(
        namespaceConverters = diHelperBox.converters,
        getWallets = getWallets,
        currenciesRepository = currenciesRepository,
        excludedBlockchains = excludedBlockchains,
    )

    @Provides
    @Singleton
    fun diHelperBox(ethNetwork: WcEthNetwork, solanaNetwork: WcSolanaNetwork) = DiHelperBox(
        handlers = setOf(
            ethNetwork,
        ),
        converters = buildMap {
            ethNetwork.namespaceKey to ethNetwork
            solanaNetwork.namespaceKey to solanaNetwork
        },
    )

    internal class DiHelperBox(
        val converters: Map<NamespaceKey, WcNamespaceConverter>,
        val handlers: Set<WcMethodHandler<WcMethod>>,
    )
}