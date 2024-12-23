package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.tap.domain.walletconnect2.domain.models.*
import kotlinx.coroutines.flow.Flow

interface LegacyWalletConnectRepository {

    val events: Flow<WalletConnectEvents>

    val activeSessions: Flow<List<WalletConnectSession>>

    val currentSessions: List<WalletConnectSession>

    fun init(projectId: String)

    fun setUserNamespaces(userNamespaces: Map<NetworkNamespace, List<Account>>)

    fun updateSessions()

    fun pair(uri: String)

    fun disconnect(topic: String)

    fun approve(userNamespaces: Map<NetworkNamespace, List<Account>>, blockchainNames: List<String>)

    fun reject()

    fun sendRequest(requestData: RequestData, result: String)

    fun rejectRequest(requestData: RequestData, error: WalletConnectError)

    fun cancelRequest(topic: String, id: Long, message: String = "")
}
