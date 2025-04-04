package com.tangem.datasource.local.walletconnect

import androidx.datastore.core.DataStore
import com.tangem.domain.walletconnect.model.WcSessionDTO
import kotlinx.coroutines.flow.Flow

internal typealias WcSessionCollection = Set<WcSessionDTO>

internal class DefaultWalletConnectStore(
    private val persistenceStore: DataStore<WcSessionCollection>,
) : WalletConnectStore {

    override val sessions: Flow<WcSessionCollection>
        get() = persistenceStore.data

    override suspend fun saveSessions(sessions: WcSessionCollection) {
        persistenceStore.updateData { data -> data.plus(sessions) }
    }

    override suspend fun removeSessions(sessions: WcSessionCollection) {
        persistenceStore.updateData { data -> data.minus(sessions) }
    }
}