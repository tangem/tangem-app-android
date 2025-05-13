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
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import timber.log.Timber

internal class DefaultWcInitializeUseCase(
    private val application: Application,
    private val sessionsManager: DefaultWcSessionsManager,
    private val networkService: DefaultWcRequestService,
    private val pairSdkDelegate: WcPairSdkDelegate,
) : WcInitializeUseCase {

    private val wcSdkObservers = mutableSetOf<WcSdkObserver>(
        sessionsManager,
        networkService,
        pairSdkDelegate,
    )

    override fun init(projectId: String) {
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
                Timber.tag(WC_TAG).i("onWcSdkInit")
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
            Timber.tag(WC_TAG).i("sdk callback onConnectionStateChange isAvailable=${state.isAvailable}")
            wcSdkObservers.forEach { it.onConnectionStateChange(state) }
        }

        override fun onError(error: Wallet.Model.Error) {
            Timber.tag(WC_TAG).e(error.throwable, "sdk callback onError")
            wcSdkObservers.forEach { it.onError(error) }
        }

        override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
            Timber.tag(WC_TAG).i("sdk callback onProposalExpired $proposal")
            wcSdkObservers.forEach { it.onProposalExpired(proposal) }
        }

        override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
            Timber.tag(WC_TAG).i("sdk callback onRequestExpired $request")
            wcSdkObservers.forEach { it.onRequestExpired(request) }
        }

        override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
            Timber.tag(WC_TAG).i("sdk callback onSessionDelete $sessionDelete")
            wcSdkObservers.forEach { it.onSessionDelete(sessionDelete) }
        }

        override fun onSessionExtend(session: Wallet.Model.Session) {
            Timber.tag(WC_TAG).i("sdk callback onSessionExtend $session")
            wcSdkObservers.forEach { it.onSessionExtend(session) }
        }

        override fun onSessionProposal(
            sessionProposal: Wallet.Model.SessionProposal,
            verifyContext: Wallet.Model.VerifyContext,
        ) {
            Timber.tag(WC_TAG).i("sdk callback onSessionProposal $sessionProposal")
            wcSdkObservers.forEach { it.onSessionProposal(sessionProposal, verifyContext) }
        }

        override fun onSessionRequest(
            sessionRequest: Wallet.Model.SessionRequest,
            verifyContext: Wallet.Model.VerifyContext,
        ) {
            Timber.tag(WC_TAG).i("sdk callback onSessionRequest $sessionRequest")
            wcSdkObservers.forEach { it.onSessionRequest(sessionRequest, verifyContext) }
        }

        override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
            Timber.tag(WC_TAG).i("sdk callback onSessionSettleResponse $settleSessionResponse")
            wcSdkObservers.forEach { it.onSessionSettleResponse(settleSessionResponse) }
        }

        override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
            Timber.tag(WC_TAG).i("sdk callback onSessionUpdateResponse $sessionUpdateResponse")
            wcSdkObservers.forEach { it.onSessionUpdateResponse(sessionUpdateResponse) }
        }
    }
}