package com.tangem.data.walletconnect.sessions

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.data.walletconnect.utils.WcSdkSessionConverter
import com.tangem.datasource.local.walletconnect.WalletConnectStore
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.domain.walletconnect.model.legacy.WalletConnectSessionsRepository
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal class DefaultWcSessionsManager constructor(
    private val store: WalletConnectStore,
    private val legacyStore: WalletConnectSessionsRepository,
    private val getWallets: GetWalletsUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val scope: CoroutineScope,
) : WcSessionsManager, WcSdkObserver {

    private val onSessionDelete = Channel<Wallet.Model.SessionDelete>(capacity = Channel.BUFFERED)
    private val oneTimeMigration = MutableStateFlow(true)

    override val sessions: Flow<Map<UserWalletId, List<WcSession>>>
        get() = store.sessions
            .transform { inStore ->
                if (oneTimeMigration.value) {
                    oneTimeMigration.value = false
                    val someMigrated = migrateLegacyStore(inStore)
                    if (someMigrated) return@transform // ignore emit, wait next one
                }
                val inSdk: List<Wallet.Model.Session> = WalletKit.getListOfActiveSessions()
                val associatedSessions: List<WcSession> = associateWithSdk(inSdk, inStore)
                val someRemove = removeUnknownSessions(inStore, associatedSessions)
                if (someRemove) return@transform // ignore emit, wait next one
                emit(associatedSessions.groupBy { it.userWalletId })
            }
            .flowOn(dispatchers.io)

    override fun onWcSdkInit() {
        oneTimeMigration.value = true
        listenOnSessionDelete()
    }

    override suspend fun saveSession(userWalletId: UserWalletId, session: WcSession) {
        store.saveSession(WcSessionDTO(session.sdkModel.topic, session.userWalletId))
    }

    override suspend fun removeSession(userWalletId: UserWalletId, session: WcSession): Either<Throwable, Unit> {
        val topic = session.sdkModel.topic
        val sdkCall = sdkDisconnectSession(topic)
        sdkCall.onLeft { return it.left() }
        suspend fun waitSdkCallback() = onSessionDelete.receiveAsFlow().first {
            val isSomeError = it is Wallet.Model.SessionDelete.Error
            val isDeleted = it is Wallet.Model.SessionDelete.Success && it.topic == topic
            isSomeError || isDeleted
        }

        val waitSdkCallback = runCatching { withTimeout(10.seconds) { waitSdkCallback() } }
        val sdkCallback = waitSdkCallback.getOrElse { return it.left() }
        return when (sdkCallback) {
            is Wallet.Model.SessionDelete.Error -> sdkCallback.error.left()
            is Wallet.Model.SessionDelete.Success -> Unit.right()
        }
    }

    override suspend fun findSessionByTopic(topic: String): WcSession? = withContext(dispatchers.io) {
        val storedSessions = store.findSessionByTopic(topic) ?: return@withContext null
        val sdkSession = WalletKit.getActiveSessionByTopic(topic) ?: return@withContext null
        WcSession(userWalletId = storedSessions.walletId, sdkModel = WcSdkSessionConverter.convert(sdkSession))
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        Timber.i("onSessionDelete: $sessionDelete")
        onSessionDelete.trySend(sessionDelete)
    }

    private suspend fun migrateLegacyStore(inNewStoreSessions: Set<WcSessionDTO>): Boolean {
        val walletIds = getWallets.invokeSync().mapTo(mutableSetOf()) { it.walletId }
        val inLegacyStoreSessions = walletIds
            .map { walletId ->
                flow { emit(legacyStore.loadSessions(walletId.stringValue).map { WcSessionDTO(it.topic, walletId) }) }
            }
            .merge()
            .reduce { accumulator, value -> accumulator.plus(value) }

        val mustSaveInNewStore = inLegacyStoreSessions.subtract(inNewStoreSessions)
        if (mustSaveInNewStore.isNotEmpty()) store.saveSessions(mustSaveInNewStore)
        return mustSaveInNewStore.isNotEmpty()
    }

    private fun associateWithSdk(
        sdkSessions: List<Wallet.Model.Session>,
        storeSessions: Set<WcSessionDTO>,
    ): List<WcSession> {
        val wcSessions = sdkSessions.mapNotNull { sdkSession ->
            val storedSessions = storeSessions.find { it.topic == sdkSession.topic }
                ?: return@mapNotNull null
            WcSession(userWalletId = storedSessions.walletId, sdkModel = WcSdkSessionConverter.convert(sdkSession))
        }
        return wcSessions
    }

    private suspend fun removeUnknownSessions(storeSessions: Set<WcSessionDTO>, wcSessions: List<WcSession>): Boolean {
        val unknownStoredSessions = storeSessions
            .filterNot { dto -> wcSessions.any { it.sdkModel.topic == dto.topic } }
        val haveSomeUnknown = unknownStoredSessions.isNotEmpty()

        if (haveSomeUnknown) {
            unknownStoredSessions.forEach { unknown ->
                legacyStore.removeSession(unknown.walletId.stringValue, unknown.topic)
            }
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