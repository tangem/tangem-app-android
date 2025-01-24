package com.tangem.tap.domain.walletconnect2.data

import android.app.Application
import arrow.core.flatten
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.events.WalletConnect
import com.tangem.tap.domain.walletconnect2.domain.LegacyWalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WcJrpcMethods
import com.tangem.tap.domain.walletconnect2.domain.WcJrpcRequestsDeserializer
import com.tangem.tap.domain.walletconnect2.domain.WcRequest
import com.tangem.tap.domain.walletconnect2.domain.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

@Suppress("LargeClass")
internal class DefaultLegacyWalletConnectRepository(
    private val application: Application,
    private val wcRequestDeserializer: WcJrpcRequestsDeserializer,
    private val analyticsHandler: AnalyticsEventHandler,
) : LegacyWalletConnectRepository {

    private var sessionProposal: Wallet.Model.SessionProposal? = null
    private var userNamespaces: Map<NetworkNamespace, List<Account>>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events: MutableSharedFlow<WalletConnectEvents> = MutableSharedFlow()
    override val events: Flow<WalletConnectEvents> = _events

    private val _activeSessions: MutableSharedFlow<List<WalletConnectSession>> = MutableSharedFlow()
    override val activeSessions: Flow<List<WalletConnectSession>> = _activeSessions

    override var currentSessions: List<WalletConnectSession> = emptyList()
        private set

    /**
     * @param projectId Project ID at https://cloud.walletconnect.com/
     */
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
            scope.launch {
                _events.emit(
                    WalletConnectEvents.SessionApprovalError(
                        WalletConnectError.ExternalApprovalError(error.throwable.message),
                    ),
                )
            }
        }

        WalletKit.initialize(
            Wallet.Params.Init(core = CoreClient),
            onSuccess = {
                val walletDelegate = defineWalletDelegate()
                WalletKit.setWalletDelegate(walletDelegate)
            },
            onError = { error ->
                Timber.e("Error while initializing Web3Wallet: $error")
                scope.launch {
                    _events.emit(
                        WalletConnectEvents.SessionApprovalError(
                            WalletConnectError.ExternalApprovalError(error.throwable.message),
                        ),
                    )
                }
            },
        )
    }

    private fun defineWalletDelegate(): WalletKit.WalletDelegate {
        return object : WalletKit.WalletDelegate {
            override fun onSessionProposal(
                sessionProposal: Wallet.Model.SessionProposal,
                verifyContext: Wallet.Model.VerifyContext,
            ) {
                // Triggered when wallet receives the session proposal sent by a Dapp
                Timber.i("sessionProposal: $sessionProposal")
                this@DefaultLegacyWalletConnectRepository.sessionProposal = sessionProposal

                if (sessionProposal.name in unsupportedDApps) {
                    Timber.i("Unsupported DApp")
                    scope.launch {
                        _events.emit(
                            WalletConnectEvents.SessionApprovalError(
                                WalletConnectError.UnsupportedDApp,
                            ),
                        )
                    }
                    return
                }

                val missingNetworks = findMissingNetworks(
                    namespaces = sessionProposal.requiredNamespaces,
                    userNamespaces = this@DefaultLegacyWalletConnectRepository.userNamespaces ?: emptyMap(),
                )

                if (missingNetworks.isNotEmpty()) {
                    Timber.i("Not added blockchains: $missingNetworks")
                    scope.launch {
                        _events.emit(
                            WalletConnectEvents.SessionApprovalError(
                                WalletConnectError.ApprovalErrorMissingNetworks(missingNetworks.toList()),
                            ),
                        )
                    }
                    return
                }

                val optionalWithoutMissingNetworks = removeMissingNetworks(
                    namespaces = sessionProposal.optionalNamespaces,
                    userNamespaces = this@DefaultLegacyWalletConnectRepository.userNamespaces ?: emptyMap(),
                )

                scope.launch {
                    _events.emit(
                        WalletConnectEvents.SessionProposal(
                            sessionProposal.name,
                            sessionProposal.description,
                            sessionProposal.url,
                            sessionProposal.icons,
                            sessionProposal.requiredNamespaces.values.flatMap { it.chains ?: emptyList() },
                            optionalWithoutMissingNetworks.toList(),
                        ),
                    )
                }
            }

            override fun onSessionRequest(
                sessionRequest: Wallet.Model.SessionRequest,
                verifyContext: Wallet.Model.VerifyContext,
            ) {
                // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
                Timber.i("sessionRequest: $sessionRequest")
                val request = wcRequestDeserializer.deserialize(
                    method = sessionRequest.request.method,
                    params = sessionRequest.request.params,
                )
                Timber.i("sessionRequestParsed: $request")

                when (request) {
                    is WcRequest.AddChain -> {
                        // we can send approval automatically, because in WC 2.0 the list of chains is approved when
                        // initial connection is established
                        sendRequest(
                            RequestData(
                                topic = sessionRequest.topic,
                                requestId = sessionRequest.request.id,
                                blockchain = sessionRequest.chainId.toString(),
                                method = WcJrpcMethods.WALLET_ADD_ETHEREUM_CHAIN.code,
                            ),
                            result = "",
                        )
                    }
                    else ->
                        scope.launch {
                            _events.emit(
                                WalletConnectEvents.SessionRequest(
                                    request = request,
                                    chainId = sessionRequest.chainId,
                                    topic = sessionRequest.topic,
                                    id = sessionRequest.request.id,
                                    metaUrl = sessionRequest.peerMetaData?.url ?: "",
                                    metaName = sessionRequest.peerMetaData?.name ?: "",
                                    method = sessionRequest.request.method,
                                ),
                            )
                        }
                }
            }

            override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
                // Triggered when the session is deleted by the peer
                if (sessionDelete is Wallet.Model.SessionDelete.Success) {
                    scope.launch {
                        _events.emit(WalletConnectEvents.SessionDeleted(sessionDelete.topic))
                        updateSessionsInternal().join()
                    }
                }
                Timber.i("onSessionDelete: $sessionDelete")
            }

            override fun onSessionExtend(session: Wallet.Model.Session) {
                Timber.i("onSessionExtend: $session")
            }

            override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
                // Triggered when wallet receives the session settlement response from Dapp
                Timber.i("onSessionSettleResponse: $settleSessionResponse")
                if (settleSessionResponse is Wallet.Model.SettledSessionResponse.Result) {
                    scope.launch {
                        _events.emit(
                            WalletConnectEvents.SessionApprovalSuccess(
                                topic = settleSessionResponse.session.topic,
                                accounts = userNamespaces?.flatMap { it.value } ?: emptyList(),
                            ),
                        )
                        updateSessionsInternal().join()
                    }
                }
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
                // Triggered when wallet receives the session update response from Dapp
                Timber.i("onSessionUpdateResponse: $sessionUpdateResponse")
                updateSessionsInternal()
            }

            override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
                // Triggered whenever the connection state is changed
                Timber.i("onConnectionStateChange: $state")
                if (state.isAvailable) updateSessionsInternal()
            }

            override fun onError(error: Wallet.Model.Error) {
                // Triggered whenever there is an issue inside the SDK
                Timber.i("onError: $error")
            }
        }
    }

    override fun setUserNamespaces(userNamespaces: Map<NetworkNamespace, List<Account>>) {
        this.userNamespaces = userNamespaces
    }

    override fun pair(uri: String) {
        WalletKit.pair(
            params = Wallet.Params.Pair(uri),
            onSuccess = {
                Timber.i("Paired successfully: $it")
            },
            onError = {
                Timber.e("Error while pairing: $it")
                scope.launch {
                    _events.emit(
                        WalletConnectEvents.PairConnectError(it.throwable),
                    )
                }
            },
        )
    }

    override fun approve(userNamespaces: Map<NetworkNamespace, List<Account>>, blockchainNames: List<String>) {
        val sessionProposal: Wallet.Model.SessionProposal = requireNotNull(this.sessionProposal)

        val userChains = userNamespaces.flatMap { namespace ->
            namespace.value.map { it.chainId to "${it.chainId}:${it.walletAddress}" }
        }.groupBy { pair -> pair.first }
            .mapValues { entry -> entry.value.map { pair -> pair.second }.toSet() }

        val preparedRequiredNamespaces = sessionProposal.requiredNamespaces
            .map { requiredNamespace ->
                val accountsRequired = requiredNamespace.value.chains
                    ?.mapNotNull { chain -> userChains[chain] }
                    ?.flatten() ?: emptyList()
                val optionalNamespace = sessionProposal.optionalNamespaces[requiredNamespace.key]
                val accountsOptional = optionalNamespace?.chains
                    ?.mapNotNull { chain -> userChains[chain] }
                    ?.flatten() ?: emptyList()

                val methods = (requiredNamespace.value.methods + (optionalNamespace?.methods ?: emptyList()))
                    .distinct()
                requiredNamespace.key to Wallet.Model.Namespace.Session(
                    accounts = (accountsRequired + accountsOptional).distinct(),
                    methods = methods,
                    events = requiredNamespace.value.events,
                )
            }.toMap()

        val sessionApproval = Wallet.Params.SessionApprove(
            proposerPublicKey = sessionProposal.proposerPublicKey,
            namespaces = preparedRequiredNamespaces.ifEmpty {
                sessionProposal.createPreparedOptionalNamespaces(userChains)
            },
        )

        Timber.i("Session approval is prepared for sending: $sessionApproval")

        WalletKit.approveSession(
            params = sessionApproval,
            onSuccess = {
                Timber.i("Approved successfully: $it")
                analyticsHandler.send(
                    WalletConnect.NewSessionEstablished(
                        dAppName = sessionProposal.name,
                        dAppUrl = sessionProposal.url,
                        blockchainNames = blockchainNames,
                    ),
                )
            },
            onError = {
                Timber.e("Error while approving: $it")
                scope.launch {
                    _events.emit(
                        WalletConnectEvents.SessionApprovalError(
                            WalletConnectError.ExternalApprovalError(it.throwable.message),
                        ),
                    )
                }
            },
        )
    }

    private fun Wallet.Model.SessionProposal.createPreparedOptionalNamespaces(
        userChains: Map<String, Set<String>>,
    ): Map<String, Wallet.Model.Namespace.Session> {
        return optionalNamespaces
            .map { optionalNamespace ->
                val accountsOptional = optionalNamespace.value.chains
                    ?.mapNotNull { chain -> userChains[chain] }
                    ?.flatten() ?: emptyList()

                val methods = optionalNamespace.value.methods
                optionalNamespace.key to Wallet.Model.Namespace.Session(
                    accounts = accountsOptional.distinct(),
                    methods = methods,
                    chains = userChains.keys
                        .filter { it.startsWith(optionalNamespace.key) }
                        .toList(),
                    events = optionalNamespace.value.events,
                )
            }
            .toMap()
    }

    override fun sendRequest(requestData: RequestData, result: String) {
        val session = currentSessions.find { it.topic == requestData.topic }
        // Add Ethereum Chain method is processed without user input, skip logging it
        if (requestData.method != WcJrpcMethods.WALLET_ADD_ETHEREUM_CHAIN.code) {
            analyticsHandler.send(
                WalletConnect.RequestHandled(
                    WalletConnect.RequestHandledParams(
                        dAppName = session?.name ?: "",
                        dAppUrl = session?.url ?: "",
                        methodName = requestData.method,
                        blockchain = requestData.blockchain,
                    ),
                ),
            )
        }

        WalletKit.respondSessionRequest(
            params = Wallet.Params.SessionRequestResponse(
                sessionTopic = requestData.topic,
                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                    id = requestData.requestId,
                    result = result,
                ),
            ),
            onSuccess = { response ->
                Timber.i("Session request responded successfully: $response")
            },
            onError = { error ->
                Timber.e(error.throwable, "Error while responging session request")

                WalletConnect.RequestHandledParams(
                    dAppName = session?.name ?: "",
                    dAppUrl = session?.url ?: "",
                    methodName = requestData.method,
                    blockchain = requestData.blockchain,
                    errorCode = WalletConnectError.ValidationError.error,
                    errorDescription = error.throwable.message,
                )
            },
        )
    }

    override fun rejectRequest(requestData: RequestData, error: WalletConnectError) {
        val session = currentSessions.find { it.topic == requestData.topic }

        analyticsHandler.send(
            WalletConnect.RequestHandled(
                WalletConnect.RequestHandledParams(
                    dAppName = session?.name ?: "",
                    dAppUrl = session?.url ?: "",
                    methodName = requestData.method,
                    blockchain = requestData.blockchain,
                    errorCode = error.toString(),
                ),
            ),
        )

        cancelRequest(requestData.topic, requestData.requestId, error.error)
    }

    override fun cancelRequest(topic: String, id: Long, message: String) {
        WalletKit.respondSessionRequest(
            params = Wallet.Params.SessionRequestResponse(
                sessionTopic = topic,
                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                    id = id,
                    code = 0,
                    message = message,
                ),
            ),
            onSuccess = {},
            onError = {},
        )
    }

    override fun reject() {
        WalletKit.rejectSession(
            params = Wallet.Params.SessionReject(
                proposerPublicKey = sessionProposal?.proposerPublicKey ?: "",
                reason = "",
            ),
            onSuccess = {
                Timber.i("Rejected successfully: $it")
            },
            onError = {
                Timber.e("Error while rejecting: $it")
            },
        )
    }

    override fun disconnect(topic: String) {
        val session = currentSessions.find { it.topic == topic }
        WalletKit.disconnectSession(
            params = Wallet.Params.SessionDisconnect(topic),
            onSuccess = {
                analyticsHandler.send(
                    WalletConnect.SessionDisconnected(
                        dAppName = session?.name ?: "",
                        dAppUrl = session?.url ?: "",
                    ),
                )
                updateSessionsInternal()
                Timber.i("Disconnected successfully: $it")
            },
            onError = {
                Timber.e("Error while disconnecting: $it")
            },
        )
    }

    fun send(topic: String, id: Long, data: String) {
        WalletKit.respondSessionRequest(
            params = Wallet.Params.SessionRequestResponse(
                sessionTopic = topic,
                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                    id = id,
                    result = data,
                ),
            ),
            onError = {},
            onSuccess = {},
        )
    }

    override fun updateSessions() {
        updateSessionsInternal()
    }

    private fun updateSessionsInternal(): Job = scope.launch {
        val availableSessions = WalletKit.getListOfActiveSessions()
            .map {
                WalletConnectSession(
                    topic = it.topic,
                    icon = it.metaData?.icons?.firstOrNull(),
                    name = it.metaData?.name,
                    url = it.metaData?.url,
                )
            }
        Timber.i("Available sessions: $availableSessions")
        currentSessions = availableSessions
        _activeSessions.emit(availableSessions)
    }

    private fun findMissingNetworks(
        namespaces: Map<String, Wallet.Model.Namespace.Proposal>,
        userNamespaces: Map<NetworkNamespace, List<Account>>,
    ): Collection<String> {
        val requiredChains = namespaces.values.flatMap { it.chains ?: emptyList() }
        val userChains = userNamespaces.flatMap { it.value.map { account -> account.chainId } }
        return requiredChains.subtract(userChains.toSet())
    }

    private fun removeMissingNetworks(
        namespaces: Map<String, Wallet.Model.Namespace.Proposal>,
        userNamespaces: Map<NetworkNamespace, List<Account>>,
    ): Collection<String> {
        val wcProvidedChains = namespaces.values.flatMap { it.chains ?: emptyList() }
        val userChains = userNamespaces.flatMap { it.value.map { account -> account.chainId } }
        return wcProvidedChains.intersect(userChains.toSet())
    }

    private companion object {

        val unsupportedDApps = listOf("dYdX", "dYdX v4", "Apex Pro", "The Sandbox")
    }
}