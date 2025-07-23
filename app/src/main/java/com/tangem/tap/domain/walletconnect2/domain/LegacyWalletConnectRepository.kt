package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.model.legacy.Account
import com.tangem.tap.domain.walletconnect2.domain.models.*
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction.OpenSession.SourceType
import kotlinx.coroutines.flow.Flow

interface LegacyWalletConnectRepository {

    val events: Flow<WalletConnectEvents>

    val activeSessions: Flow<List<WalletConnectSession>>

    val currentSessions: List<WalletConnectSession>

    fun init(projectId: String)

    fun setUserNamespaces(userNamespaces: Map<NetworkNamespace, List<Account>>)

    fun updateSessions()

    fun pair(userWalletId: UserWalletId, uri: String, source: SourceType)

    fun disconnect(topic: String)

    fun approve(userNamespaces: Map<NetworkNamespace, List<Account>>, blockchainNames: List<String>)

    fun reject()

    fun sendRequest(requestData: RequestData, result: String)

    fun rejectRequest(requestData: RequestData, error: WalletConnectError)

    fun cancelRequest(topic: String, id: Long, message: String = "")
}