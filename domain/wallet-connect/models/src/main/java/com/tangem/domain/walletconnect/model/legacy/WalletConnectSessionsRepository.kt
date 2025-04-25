package com.tangem.domain.walletconnect.model.legacy

interface WalletConnectSessionsRepository {
    suspend fun loadSessions(userWallet: String): List<Session>

    suspend fun saveSession(userWallet: String, session: Session)

    suspend fun removeSession(userWallet: String, topic: String)
}