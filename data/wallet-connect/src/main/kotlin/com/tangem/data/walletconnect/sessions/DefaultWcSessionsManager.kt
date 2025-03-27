package com.tangem.data.walletconnect.sessions

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

internal class DefaultWcSessionsManager : WcSessionsManager, WcSdkObserver {

    private val _sessions = MutableSharedFlow<Map<UserWalletId, List<WcSession>>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val sessions get() = _sessions.distinctUntilChanged()

    override suspend fun saveSessions(userWalletId: UserWalletId, session: WcSession) {
        TODO("Not yet implemented")
    }

    override suspend fun removeSessions(userWalletId: UserWalletId, session: WcSession) {
        TODO("Not yet implemented")
    }

    override suspend fun findSessionByTopic(topic: String): WcSession? {
        TODO("Not yet implemented")
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        // Triggered when the session is deleted by the peer
        Timber.i("onSessionDelete: $sessionDelete")
        // todo (wc) find session, delete and update sessions Flow
    }
}