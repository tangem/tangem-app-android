package com.tangem.data.walletconnect.sessions

import com.google.common.truth.Truth.assertThat
import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.store.WalletConnectStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WcInterruptedApprovalsTest {

    private val store: WalletConnectStore = mockk(relaxed = true)
    private val interruptedApprovals = WcInterruptedApprovals(store)

    @BeforeEach
    fun resetMocks() {
        clearMocks(store)
    }

    @Test
    fun `GIVEN approval settled in sdk WHEN complete THEN stores its session and drops the approval`() = runTest {
        // Arrange
        val approval = createApproval(pairingTopic = "pairing")
        every { store.pendingApprovalIncludingExpired } returns flowOf(setOf(approval))
        val expected = approval.session.copy(topic = "session")

        // Act
        val actual = interruptedApprovals.complete(listOf(createSdkSession(pairingTopic = "pairing", topic = "session")))

        // Assert
        assertThat(actual).containsExactly(expected)
        coVerify(exactly = 1) { store.saveSessions(setOf(expected)) }
        coVerify(exactly = 1) { store.removePendingApproval(setOf(approval)) }
    }

    @Test
    fun `GIVEN expired approval settled in sdk WHEN complete THEN its session is still stored`() = runTest {
        // Arrange
        val approval = createApproval(pairingTopic = "pairing", expiredTime = 0)
        every { store.pendingApprovalIncludingExpired } returns flowOf(setOf(approval))
        val expected = approval.session.copy(topic = "session")

        // Act
        val actual = interruptedApprovals.complete(listOf(createSdkSession(pairingTopic = "pairing", topic = "session")))

        // Assert
        assertThat(actual).containsExactly(expected)
        coVerify(exactly = 1) { store.saveSessions(setOf(expected)) }
        coVerify(exactly = 1) { store.removePendingApproval(setOf(approval)) }
    }

    @Test
    fun `GIVEN approval not settled yet WHEN complete THEN store is untouched`() = runTest {
        // Arrange
        every { store.pendingApprovalIncludingExpired } returns flowOf(setOf(createApproval(pairingTopic = "pairing")))

        // Act
        val actual = interruptedApprovals.complete(listOf(createSdkSession(pairingTopic = "other", topic = "session")))

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 0) { store.saveSessions(any()) }
        coVerify(exactly = 0) { store.removePendingApproval(any()) }
    }

    @Test
    fun `GIVEN expired approval never settled WHEN complete THEN only the approval is dropped`() = runTest {
        // Arrange
        val approval = createApproval(pairingTopic = "pairing", expiredTime = 0)
        every { store.pendingApprovalIncludingExpired } returns flowOf(setOf(approval))

        // Act
        val actual = interruptedApprovals.complete(emptyList())

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 0) { store.saveSessions(any()) }
        coVerify(exactly = 1) { store.removePendingApproval(setOf(approval)) }
    }

    @Test
    fun `GIVEN no approvals WHEN complete THEN store is untouched`() = runTest {
        // Arrange
        every { store.pendingApprovalIncludingExpired } returns flowOf(emptySet())

        // Act
        val actual = interruptedApprovals.complete(listOf(createSdkSession(pairingTopic = "pairing", topic = "session")))

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 0) { store.saveSessions(any()) }
        coVerify(exactly = 0) { store.removePendingApproval(any()) }
    }

    private fun createApproval(pairingTopic: String, expiredTime: Long = Long.MAX_VALUE) = WcPendingApprovalSessionDTO(
        pairingTopic = pairingTopic,
        session = WcSessionDTO(topic = "", walletId = UserWalletId(stringValue = "011"), url = "https://dapp.example"),
        expiredTime = expiredTime,
    )

    private fun createSdkSession(pairingTopic: String, topic: String): Wallet.Model.Session = mockk {
        every { this@mockk.pairingTopic } returns pairingTopic
        every { this@mockk.topic } returns topic
    }
}