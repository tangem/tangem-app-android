package com.tangem.data.walletconnect.sessions

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.store.WalletConnectStore
import com.tangem.data.walletconnect.store.WcSessionCollection
import com.tangem.domain.walletconnect.WC_TAG
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

/**
 * Finishes approvals whose settlement the SDK handled but the app never stored.
 *
 * The process can die after the SDK has settled a session and before the settle callback moved its pending
 * approval into the sessions store. The SDK lists settled sessions only, so an approval that matches one of them
 * by pairing topic is stored as a session here; otherwise the session would count as unknown and be disconnected.
 * An expired approval without such a session was never settled and is dropped.
 */
internal class WcInterruptedApprovals(private val store: WalletConnectStore) {

    /** Returns the sessions stored by this call. */
    suspend fun complete(sdkSessions: List<Wallet.Model.Session>): WcSessionCollection {
        val approvals = store.pendingApprovalIncludingExpired.first()
        if (approvals.isEmpty()) return emptySet()

        val completed = approvals.mapNotNull { approval ->
            val settled = sdkSessions.find { it.pairingTopic == approval.pairingTopic } ?: return@mapNotNull null
            approval to approval.session.copy(topic = settled.topic)
        }.toMap()
        val now = DateTime.now().millis
        val expired = approvals.filter { it !in completed && it.expiredTime < now }
        val completedSessions = completed.values.toSet()

        // Saving a session makes the sessions store emit, which restarts the caller's transform; the approval
        // must still be removed.
        withContext(NonCancellable) {
            if (completedSessions.isNotEmpty()) {
                TangemLogger.withTag(WC_TAG).i("Completing interrupted approvals: $completedSessions")
                store.saveSessions(completedSessions)
            }
            if (completed.isNotEmpty() || expired.isNotEmpty()) {
                store.removePendingApproval(completed.keys + expired)
            }
        }
        return completedSessions
    }
}