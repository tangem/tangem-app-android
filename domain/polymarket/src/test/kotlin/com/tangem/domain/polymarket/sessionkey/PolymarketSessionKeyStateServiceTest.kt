package com.tangem.domain.polymarket.sessionkey

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.interactor.ReconcilePolymarketSessionKeysInteractor
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSessionKeyStateServiceTest {

    private val reconcile: ReconcilePolymarketSessionKeysInteractor = mockk()
    private val service = PolymarketSessionKeyStateService(reconcile)

    @BeforeEach
    fun setup() {
        clearMocks(reconcile)
    }

    @Test
    fun `GIVEN a pending key WHEN observed THEN it is polled until it settles`() = runTest {
        // Arrange
        val pending = overview(PolymarketSessionKeyStatus.PENDING)
        val active = overview(PolymarketSessionKeyStatus.ACTIVE)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(pending.right(), pending.right(), active.right())

        // Act
        service.observe(USER_WALLET_ID).test {
            // Assert
            assertThat(awaitItem()).isEqualTo(pending.right())
            assertThat(awaitItem()).isEqualTo(active.right())
            advanceTimeBy(POLL_INTERVAL_MILLIS * 10)
            runCurrent()
            expectNoEvents()
            coVerify(exactly = 3) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN a pending key WHEN a reconcile fails THEN polling goes on until it settles`() = runTest {
        // Arrange
        val pending = overview(PolymarketSessionKeyStatus.PENDING)
        val active = overview(PolymarketSessionKeyStatus.ACTIVE)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(
            pending.right(),
            PolymarketWalletError.Network.left(),
            active.right(),
        )

        // Act
        service.observe(USER_WALLET_ID).test {
            // Assert
            assertThat(awaitItem()).isEqualTo(pending.right())
            assertThat(awaitItem()).isEqualTo(PolymarketWalletError.Network.left())
            assertThat(awaitItem()).isEqualTo(active.right())
            coVerify(exactly = 3) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN nothing settling WHEN a reconcile fails THEN it waits for a refresh`() = runTest {
        // Arrange
        coEvery { reconcile(USER_WALLET_ID) } returns PolymarketWalletError.Network.left()

        // Act
        service.observe(USER_WALLET_ID).test {
            awaitItem()
            advanceTimeBy(POLL_INTERVAL_MILLIS * 10)
            runCurrent()

            // Assert
            expectNoEvents()
            coVerify(exactly = 1) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN settled keys WHEN refresh THEN the wallet is reconciled again`() = runTest {
        // Arrange
        val active = overview(PolymarketSessionKeyStatus.ACTIVE)
        val revoked = overview(PolymarketSessionKeyStatus.REVOKED)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(active.right(), revoked.right())

        // Act
        service.observe(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(active.right())
            service.refresh(USER_WALLET_ID)

            // Assert
            assertThat(awaitItem()).isEqualTo(revoked.right())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN another wallet is refreshed WHEN observed THEN this wallet is not reconciled again`() = runTest {
        // Arrange
        coEvery { reconcile(USER_WALLET_ID) } returns overview(PolymarketSessionKeyStatus.ACTIVE).right()

        // Act
        service.observe(USER_WALLET_ID).test {
            awaitItem()
            service.refresh(UserWalletId("022"))
            runCurrent()

            // Assert
            expectNoEvents()
            coVerify(exactly = 1) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val POLL_INTERVAL_MILLIS = 3_000L
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)

        fun overview(status: PolymarketSessionKeyStatus): PolymarketSessionKeyOverview {
            val key = PolymarketSessionKey(
                address = "0x5e55000000000000000000000000000000000001",
                deviceId = "device",
                deviceName = "Pixel",
                isEnabled = true,
                status = status,
                scopes = listOf("CLOB"),
                validUntil = NOW.plusSeconds(1_000_000),
                renewAfter = null,
                authorizedAt = null,
                createdAt = NOW,
            )
            return PolymarketSessionKeyOverview(
                wallet = PolymarketWalletState(
                    depositWalletAddress = "0x2222222222222222222222222222222222222222",
                    status = PolymarketWalletStatus.READY_TO_TRADE,
                    sessionKeys = listOf(key),
                ),
                localKeyAddress = key.address,
                assessment = PolymarketSessionKeyAssessment(
                    deviceState = PolymarketSessionKeyDeviceState.OfferEnable,
                    reconciliation = PolymarketSessionKeyReconciliation.Unavailable,
                ),
            )
        }
    }
}