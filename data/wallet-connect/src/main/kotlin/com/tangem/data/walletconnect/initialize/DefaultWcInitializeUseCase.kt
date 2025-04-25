package com.tangem.data.walletconnect.initialize

import android.app.Application
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.pair.WcPairSdkDelegate
import com.tangem.data.walletconnect.request.DefaultWcRequestService
import com.tangem.data.walletconnect.sessions.DefaultWcSessionsManager
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DefaultWcInitializeUseCase(
    private val application: Application,
    private val sessionsManager: DefaultWcSessionsManager,
    private val networkService: DefaultWcRequestService,
    private val pairSdkDelegate: WcPairSdkDelegate,
    private val environmentConfigStorage: EnvironmentConfigStorage,
    private val dispatchers: CoroutineDispatcherProvider,
) : WcInitializeUseCase {

    private val wcSdkObservers = mutableSetOf<WcSdkObserver>(
        sessionsManager,
        networkService,
        pairSdkDelegate,
    )

    override suspend fun init() = withContext(dispatchers.io) {
        val projectId = environmentConfigStorage.getConfigSync().walletConnectProjectId
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=$projectId"
        val connectionType = ConnectionType.AUTOMATIC

        val appMetaData = Core.Model.AppMetaData(
            name = "Tangem",
            description = "Tangem Wallet",
            url = "tangem.com",
            icons = listOf(
                "https://user-images.githubusercontent.com/24321494/124071202-72a00900-da58-11eb-935a-dcdab21de52b.png",
            ),
            redirect = "kotlin-wallet-wc:/request", // Custom Redirect URI
        )

        CoreClient.initialize(
            relayServerUrl = serverUrl,
            connectionType = connectionType,
            application = application,
            metaData = appMetaData,
        ) { error ->
            Timber.e("Error while initializing client: $error")
        }

        WalletKit.initialize(
            Wallet.Params.Init(core = CoreClient),
            onSuccess = {
                val walletDelegate = defineWalletDelegate()
                WalletKit.setWalletDelegate(walletDelegate)
                wcSdkObservers.forEach { it.onWcSdkInit() }
            },
            onError = { error ->
                Timber.e("Error while initializing Web3Wallet: $error")
            },
        )
    }

    private fun defineWalletDelegate() = object : WalletKit.WalletDelegate {
        override val onSessionAuthenticate: ((Wallet.Model.SessionAuthenticate, Wallet.Model.VerifyContext) -> Unit)?
            get() = super.onSessionAuthenticate

        override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
            wcSdkObservers.forEach { it.onConnectionStateChange(state) }
        }

        override fun onError(error: Wallet.Model.Error) {
            wcSdkObservers.forEach { it.onError(error) }
        }

        override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
            wcSdkObservers.forEach { it.onProposalExpired(proposal) }
        }

        override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
            wcSdkObservers.forEach { it.onRequestExpired(request) }
        }

        override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
            wcSdkObservers.forEach { it.onSessionDelete(sessionDelete) }
        }

        override fun onSessionExtend(session: Wallet.Model.Session) {
            wcSdkObservers.forEach { it.onSessionExtend(session) }
        }

        override fun onSessionProposal(
            sessionProposal: Wallet.Model.SessionProposal,
            verifyContext: Wallet.Model.VerifyContext,
        ) {
            wcSdkObservers.forEach { it.onSessionProposal(sessionProposal, verifyContext) }
        }

        override fun onSessionRequest(
            sessionRequest: Wallet.Model.SessionRequest,
            verifyContext: Wallet.Model.VerifyContext,
        ) {
            wcSdkObservers.forEach { it.onSessionRequest(sessionRequest, verifyContext) }
        }

        override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
            wcSdkObservers.forEach { it.onSessionSettleResponse(settleSessionResponse) }
        }

        override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
            wcSdkObservers.forEach { it.onSessionUpdateResponse(sessionUpdateResponse) }
        }
    }
}