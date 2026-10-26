package com.tangem.data.walletconnect.store

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.featuretoggle.WalletConnectFeatureToggles
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.test.core.datastore.MockStateDataStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultWalletConnectStoreTest {

    private val featureToggles: WalletConnectFeatureToggles = mockk()

    private val actual = createApproval(pairingTopic = "actual", expiredTime = Long.MAX_VALUE)
    private val expired = createApproval(pairingTopic = "expired", expiredTime = 0)

    @Test
    fun `GIVEN recovery enabled WHEN pendingApproval read THEN expired approvals are hidden but kept`() = runTest {
        // Arrange
        every { featureToggles.isOnDemandInitEnabled } returns true
        val store = createStore(setOf(actual, expired))

        // Act
        val visible = store.pendingApproval.first()

        // Assert
        assertThat(visible).containsExactly(actual)
        assertThat(store.pendingApprovalIncludingExpired.first()).containsExactly(actual, expired)
    }

    @Test
    fun `GIVEN recovery disabled WHEN pendingApproval read THEN expired approvals are removed`() = runTest {
        // Arrange
        every { featureToggles.isOnDemandInitEnabled } returns false
        val store = createStore(setOf(actual, expired))

        // Act
        val visible = store.pendingApproval.first()

        // Assert
        assertThat(visible).containsExactly(actual)
        assertThat(store.pendingApprovalIncludingExpired.first()).containsExactly(actual)
    }

    private fun createStore(approvals: Set<WcPendingApprovalSessionDTO>) = DefaultWalletConnectStore(
        persistenceStore = MockStateDataStore(emptySet()),
        pendingApprovalSessionsStore = MockStateDataStore(approvals),
        featureToggles = featureToggles,
    )

    private fun createApproval(pairingTopic: String, expiredTime: Long) = WcPendingApprovalSessionDTO(
        pairingTopic = pairingTopic,
        session = WcSessionDTO(topic = "", walletId = UserWalletId(stringValue = "011"), url = "https://dapp.example"),
        expiredTime = expiredTime,
    )
}