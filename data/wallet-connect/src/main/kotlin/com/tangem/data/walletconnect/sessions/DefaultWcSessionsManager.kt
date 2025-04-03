package com.tangem.data.walletconnect.sessions

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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DefaultWcSessionsManager constructor(
    private val store: WalletConnectStore,
    private val legacyStore: WalletConnectSessionsRepository,
    private val getWallets: GetWalletsUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val scope: CoroutineScope,
) : WcSessionsManager, WcSdkObserver {

    override val sessions: Flow<Map<UserWalletId, List<WcSession>>>
        get() = store.sessions
            .onEach(::migrateLegacyStore)
            .map(::associateWithSdk)
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override suspend fun saveSession(userWalletId: UserWalletId, session: WcSession) {
        store.saveSession(WcSessionDTO(session.sdkModel.topic, session.userWalletId))
    }

    override suspend fun removeSession(userWalletId: UserWalletId, session: WcSession) {
        store.removeSession(WcSessionDTO(session.sdkModel.topic, session.userWalletId))
    }

    override suspend fun findSessionByTopic(topic: String): WcSession? = withContext(dispatchers.io) {
        val storedSessions = store.findSessionByTopic(topic) ?: return@withContext null
        val sdkSession = WalletKit.getActiveSessionByTopic(topic) ?: return@withContext null
        WcSession(userWalletId = storedSessions.walletId, sdkModel = WcSdkSessionConverter.convert(sdkSession))
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        if (sessionDelete !is Wallet.Model.SessionDelete.Success) return
        Timber.i("onSessionDelete: $sessionDelete")
        scope.launch {
            val storedSessions = store.findSessionByTopic(sessionDelete.topic) ?: return@launch
            store.removeSession(storedSessions)
        }
    }

    private suspend fun migrateLegacyStore(inNewStoreSessions: Set<WcSessionDTO>) {
        val walletIds = getWallets.invokeSync().mapTo(mutableSetOf()) { it.walletId }
        val inLegacyStoreSessions = walletIds
            .map { walletId ->
                flow { emit(legacyStore.loadSessions(walletId.stringValue).map { WcSessionDTO(it.topic, walletId) }) }
            }
            .merge()
            .first()

        val mustSaveInNewStore = inLegacyStoreSessions.subtract(inNewStoreSessions)
        if (mustSaveInNewStore.isNotEmpty()) store.saveSessions(mustSaveInNewStore)
    }

    private suspend fun associateWithSdk(storeSessions: Set<WcSessionDTO>): Map<UserWalletId, List<WcSession>> {
        val sdkSessions = WalletKit.getListOfActiveSessions()
        val wcSessions = sdkSessions.mapNotNull { sdkSession ->
            val storedSessions = storeSessions.find { it.topic == sdkSession.topic }
                ?: return@mapNotNull null
            WcSession(userWalletId = storedSessions.walletId, sdkModel = WcSdkSessionConverter.convert(sdkSession))
        }

        val unknownStoredSessions = storeSessions
            .filterNot { dto -> wcSessions.any { it.sdkModel.topic == dto.topic } }

        if (unknownStoredSessions.isNotEmpty()) {
            unknownStoredSessions.forEach { unknown ->
                legacyStore.removeSession(unknown.walletId.stringValue, unknown.topic)
            }
            store.removeSessions(unknownStoredSessions.toSet())
        }

        return wcSessions.groupBy { it.userWalletId }
    }
}