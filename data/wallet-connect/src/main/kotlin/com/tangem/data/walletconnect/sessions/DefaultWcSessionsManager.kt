package com.tangem.data.walletconnect.sessions

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.pair.AssociateNetworksDelegate
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.data.walletconnect.utils.WcSdkSessionConverter
import com.tangem.datasource.local.walletconnect.WalletConnectStore
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.domain.walletconnect.model.legacy.WalletConnectSessionsRepository
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

internal class DefaultWcSessionsManager(
    private val store: WalletConnectStore,
    private val legacyStore: WalletConnectSessionsRepository,
    private val getWallets: GetWalletsUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val associateNetworks: AssociateNetworksDelegate,
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
    }

    override suspend fun saveSession(session: WcSession) {
        store.saveSession(WcSessionDTO(session.sdkModel.topic, session.wallet.walletId, session.securityStatus))
    }

    override suspend fun removeSession(session: WcSession): Either<Throwable, Unit> {
        val topic = session.sdkModel.topic
        val sdkCall = sdkDisconnectSession(topic)
            .onRight { onSessionDelete.trySend(Wallet.Model.SessionDelete.Success(topic = topic, reason = "")) }
        return sdkCall
    }

    override suspend fun findSessionByTopic(topic: String): WcSession? = withContext(dispatchers.io) {
        val storedSession = sessions.firstOrNull()
            ?.values?.flatten()
            ?.firstOrNull { it.sdkModel.topic == topic }
            ?: return@withContext null
        val sdkSession = WalletKit.getActiveSessionByTopic(topic) ?: return@withContext null
        val wallet = storedSession.wallet
        val networks = associateNetworks.associate(wallet, sdkSession.namespaces)
        WcSession(
            wallet = wallet,
            sdkModel = WcSdkSessionConverter.convert(sdkSession),
            securityStatus = storedSession.securityStatus,
            networks = networks,
        )
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
        val wcSessions = inStore.mapNotNull { session ->
            val wallet = wallets.find { it.walletId == session.walletId } ?: return@mapNotNull null
            val sdkSession = inSdk.find { it.topic == session.topic } ?: return@mapNotNull null
            val networks = associateNetworks.associate(wallet, sdkSession.namespaces)
            WcSession(
                wallet = wallet,
                sdkModel = WcSdkSessionConverter.convert(sdkSession),
                securityStatus = session.securityStatus,
                networks = networks,
            )
        }
        return wcSessions
    }

    private suspend fun removeUnknownSessions(storeSessions: Set<WcSessionDTO>, wcSessions: List<WcSession>): Boolean {
        val unknownStoredSessions = storeSessions
            .filterNot { dto -> wcSessions.any { it.sdkModel.topic == dto.topic } }
        val haveSomeUnknown = unknownStoredSessions.isNotEmpty()

        if (haveSomeUnknown) {
            store.removeSessions(unknownStoredSessions.toSet())
        }
        return haveSomeUnknown
    }

    private suspend fun sdkDisconnectSession(topic: String): Either<Throwable, Unit> {
        return suspendCancellableCoroutine { continuation ->
            WalletKit.disconnectSession(
                params = Wallet.Params.SessionDisconnect(topic),
                onSuccess = { continuation.resume(Unit.right()) },
                onError = { continuation.resume(it.throwable.left()) },
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