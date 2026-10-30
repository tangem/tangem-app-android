package com.tangem.data.walletconnect.store

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultWalletConnectStoreTest {

    private val actual = createApproval(pairingTopic = "actual", expiredTime = Long.MAX_VALUE)
    private val expired = createApproval(pairingTopic = "expired", expiredTime = 0)

    @Test
    fun `GIVEN expired approval WHEN pendingApproval read THEN expired approvals are hidden but kept`() = runTest {
        // Arrange
        val store = createStore(setOf(actual, expired))

        // Act
        val visible = store.pendingApproval.first()

        // Assert
        assertThat(visible).containsExactly(actual)
        assertThat(store.pendingApprovalIncludingExpired.first()).containsExactly(actual, expired)
    }

    private fun createStore(approvals: Set<WcPendingApprovalSessionDTO>) = DefaultWalletConnectStore(
        persistenceStore = MockStateDataStore(emptySet()),
        pendingApprovalSessionsStore = MockStateDataStore(approvals),
    )

    private fun createApproval(pairingTopic: String, expiredTime: Long) = WcPendingApprovalSessionDTO(
        pairingTopic = pairingTopic,
        session = WcSessionDTO(topic = "", walletId = UserWalletId(stringValue = "011"), url = "https://dapp.example"),
        expiredTime = expiredTime,
    )
}