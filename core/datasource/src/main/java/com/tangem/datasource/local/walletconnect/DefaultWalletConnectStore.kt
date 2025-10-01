package com.tangem.datasource.local.walletconnect

import androidx.datastore.core.DataStore
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import org.joda.time.DateTime

internal typealias WcSessionCollection = Set<WcSessionDTO>

internal class DefaultWalletConnectStore(
    private val persistenceStore: DataStore<WcSessionCollection>,
    private val pendingApprovalSessionsStore: DataStore<Set<WcPendingApprovalSessionDTO>>,
) : WalletConnectStore {

    override val sessions: Flow<WcSessionCollection>
        get() = persistenceStore.data

    override val pendingApproval: Flow<Set<WcPendingApprovalSessionDTO>>
        get() = pendingApprovalSessionsStore.data
            .transform { pendingApprovalSet ->
                val now = DateTime.now()
                val expired = pendingApprovalSet
                    .filterTo(mutableSetOf()) { it.expiredTime < now.millis }
                val shouldSomeClear = expired.isNotEmpty()
                val actualData = if (shouldSomeClear) {
                    removePendingApproval(expired)
                } else {
                    pendingApprovalSet
                }
                emit(actualData)
            }
            .distinctUntilChanged()

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