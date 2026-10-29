package com.tangem.data.walletconnect.store

import androidx.datastore.core.DataStore
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.joda.time.DateTime

internal typealias WcSessionCollection = Set<WcSessionDTO>

internal class DefaultWalletConnectStore(
    private val persistenceStore: DataStore<WcSessionCollection>,
    private val pendingApprovalSessionsStore: DataStore<Set<WcPendingApprovalSessionDTO>>,
) : WalletConnectStore {

    override val sessions: Flow<WcSessionCollection>
        get() = persistenceStore.data

    override val pendingApproval: Flow<Set<WcPendingApprovalSessionDTO>>
        // Expired approvals are only hidden here: one of them may have been settled right before the process
        // died, so they are removed where the SDK sessions are known.
        get() = pendingApprovalSessionsStore.data
            .map { pendingApprovalSet ->
                val now = DateTime.now().millis
                pendingApprovalSet.filterTo(mutableSetOf()) { it.expiredTime >= now }
            }
            .distinctUntilChanged()

    override val pendingApprovalIncludingExpired: Flow<Set<WcPendingApprovalSessionDTO>>
        get() = pendingApprovalSessionsStore.data

    override suspend fun saveSessions(sessions: WcSessionCollection) {
        persistenceStore.updateData { data -> data.plus(sessions) }
    }

    override suspend fun removeSessions(sessions: WcSessionCollection) {
        persistenceStore.updateData { data -> data.minus(sessions) }
    }

    override suspend fun savePendingApproval(
        sessions: Set<WcPendingApprovalSessionDTO>,
    ): Set<WcPendingApprovalSessionDTO> {
        return pendingApprovalSessionsStore.updateData { data -> data.plus(sessions) }
    }

    override suspend fun removePendingApproval(
        sessions: Set<WcPendingApprovalSessionDTO>,
    ): Set<WcPendingApprovalSessionDTO> {
        return pendingApprovalSessionsStore.updateData { data -> data.minus(sessions) }
    }
}