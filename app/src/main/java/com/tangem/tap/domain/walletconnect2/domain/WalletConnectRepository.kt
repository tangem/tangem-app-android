package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.tap.domain.walletconnect2.domain.models.Account
import com.tangem.tap.domain.walletconnect2.domain.models.NetworkNamespace
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectEvents
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectSession
import kotlinx.coroutines.flow.Flow

interface WalletConnectRepository {

    val events: Flow<WalletConnectEvents>

    val activeSessions: Flow<List<WalletConnectSession>>

    fun init(projectId: String)

    fun updateSessions()

    fun pair(uri: String)

    fun disconnect(topic: String)

    fun approve(userNamespaces: Map<NetworkNamespace, List<Account>>)

    fun reject()

    fun sendRequest(topic: String, id: Long, result: String)

    fun rejectRequest(topic: String, id: Long)
}