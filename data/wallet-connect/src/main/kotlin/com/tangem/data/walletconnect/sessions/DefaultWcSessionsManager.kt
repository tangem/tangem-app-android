package com.tangem.data.walletconnect.sessions

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.utils.*
import com.tangem.datasource.local.walletconnect.WalletConnectStore
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.domain.walletconnect.model.legacy.WalletConnectSessionsRepository
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.joda.time.DateTime
import timber.log.Timber
import kotlin.coroutines.resume

@Suppress("LongParameterList")
internal class DefaultWcSessionsManager(
    private val store: WalletConnectStore,
    private val legacyStore: WalletConnectSessionsRepository,
    private val getWallets: GetWalletsUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val wcNetworksConverter: WcNetworksConverter,
    private val analytics: AnalyticsEventHandler,
    private val scope: CoroutineScope,
) : WcSessionsManager, WcSdkObserver {

    private val onSessionDelete = Channel<Wallet.Model.SessionDelete>(capacity = Channel.BUFFERED)
    private val oneTimeMigration = MutableStateFlow(true)

    override val sessions: Flow<Map<UserWallet, List<WcSession>>>
        get() = combine(getWallets(), store.sessions) { wallets, inStore -> wallets to inStore }
            .transform { pair ->
                val (wallets, inStore) = pair
                val inSdk: List<Wallet.Model.Session> = WalletKit.getListOfActiveSessions()
                if (oneTimeMigration.value) {
                    oneTimeMigration.value = false
                    val someMigrated = migrateLegacyStore(inStore, inSdk, wallets)
                    if (someMigrated) return@transform // ignore emit, wait next one
                }
                val associatedSessions: List<WcSession> = associate(inSdk, inStore, wallets)
                val someRemove = removeUnknownSessions(inStore, associatedSessions)
                if (someRemove) return@transform // ignore emit, wait next one
                emit(associatedSessions.groupBy { it.wallet })
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override fun onWcSdkInit() {
        oneTimeMigration.value = true
        listenOnSessionDelete()
        extendSessions()
    }

    override suspend fun saveSession(session: WcSession) {
        store.saveSession(
            WcSessionDTO(
                topic = session.sdkModel.topic,
                walletId = session.wallet.walletId,
                securityStatus = session.securityStatus,
                connectingTime = session.connectingTime ?: DateTime.now().millis,
            ),
        )
    }

    override suspend fun removeSession(session: WcSession): Either<Throwable, Unit> {
        val topic = session.sdkModel.topic
        val sdkCall = sdkDisconnectSession(topic)
            .onRight { onSessionDelete.trySend(Wallet.Model.SessionDelete.Success(topic = topic, reason = "")) }
        analytics.send(WcAnalyticEvents.SessionDisconnected(session.sdkModel.appMetaData))
        return sdkCall
    }

    override suspend fun findSessionByTopic(topic: String): WcSession? = withContext(dispatchers.io) {
        sessions.firstOrNull()
            ?.values?.flatten()
            ?.firstOrNull { it.sdkModel.topic == topic }
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        Timber.i("onSessionDelete: $sessionDelete")
        onSessionDelete.trySend(sessionDelete)
    }

    private suspend fun migrateLegacyStore(
        inNewStore: Set<WcSessionDTO>,
        inSdk: List<Wallet.Model.Session>,
        wallets: List<UserWallet>,
    ): Boolean {
        val walletIds = wallets.map { wallet -> wallet.walletId }
        val inLegacyStore = walletIds
            .map { walletId ->
                flow {
                    emit(
                        legacyStore.loadSessions(walletId.stringValue).map {
                            WcSessionDTO(it.topic, walletId, CheckDAppResult.FAILED_TO_VERIFY)
                        },
                    )
                }
            }
            .merge()
            .reduce { accumulator, value -> accumulator.plus(value) }
            // migrate only active legacySessions
            .filter { legacySession -> inSdk.any { inSdkSession -> inSdkSession.topic == legacySession.topic } }

        val mustSaveInNewStore = inLegacyStore.subtract(inNewStore)
        if (mustSaveInNewStore.isNotEmpty()) store.saveSessions(mustSaveInNewStore)
        return mustSaveInNewStore.isNotEmpty()
    }

    private suspend fun associate(
        inSdk: List<Wallet.Model.Session>,
        inStore: Set<WcSessionDTO>,
        wallets: List<UserWallet>,
    ): List<WcSession> {
        val wcSessions = inStore.mapNotNull { storeSession ->
            val wallet = wallets.find { it.walletId == storeSession.walletId } ?: return@mapNotNull null
            val sdkSession = inSdk.find { it.topic == storeSession.topic } ?: return@mapNotNull null
            val networks = wcNetworksConverter.findWalletNetworks(wallet, sdkSession)
            WcSession(
                wallet = wallet,
                sdkModel = WcSdkSessionConverter.convert(sdkSession),
                securityStatus = storeSession.securityStatus,
                networks = networks,
                connectingTime = storeSession.connectingTime,
                showWalletInfo = wallets.size > 1,
            )
        }
        return wcSessions
    }

    private suspend fun removeUnknownSessions(storeSessions: Set<WcSessionDTO>, wcSessions: List<WcSession>): Boolean {
        val unknownStoredSessions = storeSessions
            .filterNot { dto -> wcSessions.any { it.sdkModel.topic == dto.topic } }
        val haveSomeUnknown = unknownStoredSessions.isNotEmpty()

        if (haveSomeUnknown) {
            Timber.tag(WC_TAG).i("removeUnknownSessions $unknownStoredSessions")
            store.removeSessions(unknownStoredSessions.toSet())
        }
        return haveSomeUnknown
    }

    private fun extendSessions() {
        scope.launch(dispatchers.io) {
            val topics: List<String> = WalletKit.getListOfActiveSessions().map { it.topic }
            val jobs = topics.map { topic -> launch { sdkSessionExtend(topic) } }
            jobs.joinAll()
        }
    }

    private suspend fun sdkDisconnectSession(topic: String): Either<Throwable, Unit> {
        return suspendCancellableCoroutine { continuation ->
            WalletKit.disconnectSession(
                params = Wallet.Params.SessionDisconnect(topic),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    private suspend fun sdkSessionExtend(topic: String): Either<Throwable, Unit> {
        return suspendCancellableCoroutine { continuation ->
            WalletKit.extendSession(
                params = Wallet.Params.SessionExtend(topic),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    private fun listenOnSessionDelete() {
        onSessionDelete.receiveAsFlow()
            .filterIsInstance<Wallet.Model.SessionDelete.Success>()
            .mapNotNull { sessionDelete -> store.findSessionByTopic(sessionDelete.topic) }
            .onEach { sessionDto -> store.removeSession(sessionDto) }
            .flowOn(dispatchers.io)
            .launchIn(scope)
    }
}