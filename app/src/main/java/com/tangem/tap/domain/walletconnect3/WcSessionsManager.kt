package com.tangem.tap.domain.walletconnect3

import com.reown.walletkit.client.Wallet
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

interface WcSessionsManager {
    val sessions: Flow<Map<UserWalletId, List<WcSession>>>
    suspend fun saveSessions(userWalletId: UserWalletId, session: WcSession)
    suspend fun removeSessions(userWalletId: UserWalletId, session: WcSession)
    suspend fun finsSessionByTopic(topic: String): WcSession?
}

// todo(wc) create our clone model?
typealias WcSdkSession = Wallet.Model.Session

data class WcSession(
    val userWalletId: UserWalletId,
    val sdkModel: WcSdkSession,
)

fun Wallet.Model.Session.toDomain(userWallet: UserWallet): WcSession = WcSession(
    userWalletId = userWallet.walletId,
    sdkModel = this,
)

class DefaultWcSessionsManager : WcSessionsManager, WcSdkObserver {

    private val _sessions = MutableSharedFlow<Map<UserWalletId, List<WcSession>>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val sessions: Flow<Map<UserWalletId, List<WcSession>>>
        get() = _sessions.distinctUntilChanged()

    override suspend fun saveSessions(userWalletId: UserWalletId, session: WcSession) {
        TODO("Not yet implemented")
    }

    override suspend fun removeSessions(userWalletId: UserWalletId, session: WcSession) {
        TODO("Not yet implemented")
    }

    override suspend fun finsSessionByTopic(topic: String): WcSession? {
        TODO("Not yet implemented")
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        // Triggered when the session is deleted by the peer
        Timber.i("onSessionDelete: $sessionDelete")
        // todo (wc) find session, delete and update sessions Flow
    }
}
