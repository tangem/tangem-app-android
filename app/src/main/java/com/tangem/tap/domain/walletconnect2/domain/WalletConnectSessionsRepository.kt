package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.tap.domain.walletconnect2.domain.models.Session

interface WalletConnectSessionsRepository {
    suspend fun loadSessions(userWallet: String): List<Session>

    suspend fun saveSession(userWallet: String, session: Session)

    suspend fun removeSession(userWallet: String, topic: String)
}