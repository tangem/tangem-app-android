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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSessionKeyStateServiceTest {

    private val reconcile: ReconcilePolymarketSessionKeysInteractor = mockk()

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
        service().observe(USER_WALLET_ID).test {
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
    fun `GIVEN a pending key WHEN a poll fails THEN polling goes on quietly until it settles`() = runTest {
        // Arrange
        val pending = overview(PolymarketSessionKeyStatus.PENDING)
        val active = overview(PolymarketSessionKeyStatus.ACTIVE)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(
            pending.right(),
            PolymarketWalletError.Network.left(),
            active.right(),
        )

        // Act
        service().observe(USER_WALLET_ID).test {
            // Assert
            assertThat(awaitItem()).isEqualTo(pending.right())
            assertThat(awaitItem()).isEqualTo(active.right())
            coVerify(exactly = 3) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN the first load fails WHEN observed THEN the error is emitted and it waits for a refresh`() = runTest {
        // Arrange
        coEvery { reconcile(USER_WALLET_ID) } returns PolymarketWalletError.Network.left()

        // Act
        service().observe(USER_WALLET_ID).test {
            val actual = awaitItem()
            advanceTimeBy(POLL_INTERVAL_MILLIS * 10)
            runCurrent()

            // Assert
            assertThat(actual).isEqualTo(PolymarketWalletError.Network.left())
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

        val service = service()

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
    fun `GIVEN a loaded wallet WHEN a refresh fails THEN the error is not emitted and the last state is kept`() =
        runTest {
            // Arrange
            val active = overview(PolymarketSessionKeyStatus.ACTIVE)
            coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(
                active.right(),
                PolymarketWalletError.Network.left(),
            )
            val service = service()

            service.observe(USER_WALLET_ID).test {
                awaitItem()

                // Act
                service.refresh(USER_WALLET_ID)
                runCurrent()

                // Assert
                expectNoEvents()
                coVerify(exactly = 2) { reconcile(USER_WALLET_ID) }
                assertThat(service.observe(USER_WALLET_ID).first()).isEqualTo(active.right())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN another wallet is refreshed WHEN observed THEN this wallet is not reconciled again`() = runTest {
        // Arrange
        coEvery { reconcile(USER_WALLET_ID) } returns overview(PolymarketSessionKeyStatus.ACTIVE).right()
        val service = service()

        // Act
        service.observe(USER_WALLET_ID).test {
            awaitItem()
            service.refresh(OTHER_WALLET_ID)
            runCurrent()

            // Assert
            expectNoEvents()
            coVerify(exactly = 1) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN two subscribers of one wallet WHEN refresh THEN the wallet is reconciled once per cycle`() = runTest {
        // Arrange
        val active = overview(PolymarketSessionKeyStatus.ACTIVE)
        val revoked = overview(PolymarketSessionKeyStatus.REVOKED)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(active.right(), revoked.right())
        val service = service()

        service.observe(USER_WALLET_ID).test {
            val second = service.observe(USER_WALLET_ID).testIn(backgroundScope)
            assertThat(awaitItem()).isEqualTo(active.right())
            assertThat(second.awaitItem()).isEqualTo(active.right())

            // Act
            service.refresh(USER_WALLET_ID)

            // Assert
            assertThat(awaitItem()).isEqualTo(revoked.right())
            assertThat(second.awaitItem()).isEqualTo(revoked.right())
            coVerify(exactly = 2) { reconcile(USER_WALLET_ID) }
            second.cancelAndIgnoreRemainingEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN an observed wallet WHEN a late subscriber joins THEN it gets the cached value without a reconcile`() =
        runTest {
            // Arrange
            val active = overview(PolymarketSessionKeyStatus.ACTIVE)
            coEvery { reconcile(USER_WALLET_ID) } returns active.right()
            val service = service()

            service.observe(USER_WALLET_ID).test {
                awaitItem()

                // Act
                val actual = service.observe(USER_WALLET_ID).first()

                // Assert
                assertThat(actual).isEqualTo(active.right())
                coVerify(exactly = 1) { reconcile(USER_WALLET_ID) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN two observed wallets WHEN both are refreshed at once THEN both are reconciled again`() = runTest {
        // Arrange
        coEvery { reconcile(any()) } returns overview(PolymarketSessionKeyStatus.ACTIVE).right()
        val service = service()
        backgroundScope.launch { service.observe(USER_WALLET_ID).collect() }
        backgroundScope.launch { service.observe(OTHER_WALLET_ID).collect() }
        runCurrent()

        // Act
        service.refresh(USER_WALLET_ID)
        service.refresh(OTHER_WALLET_ID)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { reconcile(USER_WALLET_ID) }
        coVerify(exactly = 2) { reconcile(OTHER_WALLET_ID) }
    }

    @Test
    fun `GIVEN a key that never settles WHEN the polls run out THEN polling stops until a refresh`() = runTest {
        // Arrange
        var reconciles = 0
        coEvery { reconcile(USER_WALLET_ID) } coAnswers {
            reconciles++
            overview(PolymarketSessionKeyStatus.PENDING).right()
        }
        val service = service()
        val maxPolls = 100

        service.observe(USER_WALLET_ID).test {
            awaitItem()

            // Act
            advanceTimeBy(POLL_INTERVAL_MILLIS * (maxPolls + 10))
            runCurrent()

            // Assert
            assertThat(reconciles).isEqualTo(maxPolls + 1)

            // Act
            service.refresh(USER_WALLET_ID)
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()

            // Assert
            assertThat(reconciles).isEqualTo(maxPolls + 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN a reconcile throws WHEN observed THEN the loop restarts and delivers the next value`() = runTest {
        // Arrange
        val active = overview(PolymarketSessionKeyStatus.ACTIVE)
        val revoked = overview(PolymarketSessionKeyStatus.REVOKED)
        var reconciles = 0
        coEvery { reconcile(USER_WALLET_ID) } coAnswers {
            reconciles++
            when (reconciles) {
                1 -> error("keystore unavailable")
                2 -> active.right()
                else -> revoked.right()
            }
        }
        val service = service()

        // Act
        service.observe(USER_WALLET_ID).test {
            // Assert
            assertThat(awaitItem()).isEqualTo(active.right())
            cancelAndIgnoreRemainingEvents()
        }
        service.observe(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(active.right())
            service.refresh(USER_WALLET_ID)
            assertThat(awaitItem()).isEqualTo(revoked.right())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN an assessment that changes later WHEN that moment comes THEN the wallet is reconciled again`() =
        runTest {
            // Arrange
            val changeAt = NOW.plus(Duration.ofMinutes(15))
            val tradable = overview(PolymarketSessionKeyStatus.ACTIVE, isTradable = true, nextChangeAt = changeAt)
            val untradable = overview(PolymarketSessionKeyStatus.ACTIVE)
            coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(tradable.right(), untradable.right())
            val service = service()

            service.observe(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(tradable.right())

                // Act
                advanceTimeBy(Duration.ofMinutes(15).toMillis() - 1)
                runCurrent()

                // Assert
                expectNoEvents()
                coVerify(exactly = 1) { reconcile(USER_WALLET_ID) }

                // Act
                advanceTimeBy(1)
                runCurrent()

                // Assert
                assertThat(awaitItem()).isEqualTo(untradable.right())
                coVerify(exactly = 2) { reconcile(USER_WALLET_ID) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN an assessment that time no longer changes WHEN idle THEN it waits for a refresh`() = runTest {
        // Arrange
        val renewalRequired = overview(
            PolymarketSessionKeyStatus.ACTIVE,
            nextChangeAt = NOW.plus(Duration.ofMinutes(5)),
        )
        val expired = overview(PolymarketSessionKeyStatus.EXPIRED)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(renewalRequired.right(), expired.right())
        val service = service()

        service.observe(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(renewalRequired.right())

            // Act
            advanceTimeBy(Duration.ofMinutes(5).toMillis())
            runCurrent()

            // Assert
            assertThat(awaitItem()).isEqualTo(expired.right())
            advanceTimeBy(Duration.ofDays(1).toMillis())
            runCurrent()
            expectNoEvents()
            coVerify(exactly = 2) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN the next change elapsed WHEN its reconciles fail THEN they are retried with backoff until a Right`() =
        runTest {
            // Arrange
            val aboutToChange = overview(
                PolymarketSessionKeyStatus.ACTIVE,
                isTradable = true,
                nextChangeAt = NOW.plus(Duration.ofMinutes(1)),
            )
            val changed = overview(PolymarketSessionKeyStatus.ACTIVE)
            coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(
                aboutToChange.right(),
                PolymarketWalletError.Network.left(),
                PolymarketWalletError.Network.left(),
                changed.right(),
            )

            service().observe(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(aboutToChange.right())

                // Act
                advanceTimeBy(Duration.ofMinutes(1).toMillis())
                runCurrent()
                advanceTimeBy(POLL_INTERVAL_MILLIS - 1)
                runCurrent()

                // Assert
                coVerify(exactly = 2) { reconcile(USER_WALLET_ID) }

                // Act
                advanceTimeBy(1)
                runCurrent()
                advanceTimeBy(POLL_INTERVAL_MILLIS * 2 - 1)
                runCurrent()

                // Assert
                coVerify(exactly = 3) { reconcile(USER_WALLET_ID) }
                expectNoEvents()

                // Act
                advanceTimeBy(1)
                runCurrent()

                // Assert
                assertThat(awaitItem()).isEqualTo(changed.right())
                coVerify(exactly = 4) { reconcile(USER_WALLET_ID) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN the next change elapsed WHEN its reconciles keep failing THEN retries go on every minute`() = runTest {
        // Arrange
        val aboutToChange = overview(PolymarketSessionKeyStatus.ACTIVE, nextChangeAt = NOW.plus(Duration.ofMinutes(1)))
        var reconciles = 0
        coEvery { reconcile(USER_WALLET_ID) } coAnswers {
            reconciles++
            if (reconciles == 1) aboutToChange.right() else PolymarketWalletError.Network.left()
        }

        service().observe(USER_WALLET_ID).test {
            awaitItem()

            // Act
            advanceTimeBy(Duration.ofHours(1).toMillis())
            runCurrent()
            val afterOneHour = reconciles
            advanceTimeBy(Duration.ofMinutes(1).toMillis())
            runCurrent()

            // Assert
            assertThat(afterOneHour).isEqualTo(64)
            assertThat(reconciles).isEqualTo(65)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN a backoff after an elapsed change WHEN a refresh also fails THEN retrying goes on`() = runTest {
        // Arrange
        val aboutToChange = overview(PolymarketSessionKeyStatus.ACTIVE, nextChangeAt = NOW.plus(Duration.ofMinutes(1)))
        val changed = overview(PolymarketSessionKeyStatus.ACTIVE, isTradable = true)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(
            aboutToChange.right(),
            PolymarketWalletError.Network.left(),
            PolymarketWalletError.Network.left(),
            changed.right(),
        )
        val service = service()

        service.observe(USER_WALLET_ID).test {
            awaitItem()
            advanceTimeBy(Duration.ofMinutes(1).toMillis())
            runCurrent()

            // Act
            service.refresh(USER_WALLET_ID)
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS * 2)
            runCurrent()

            // Assert
            assertThat(awaitItem()).isEqualTo(changed.right())
            coVerify(exactly = 4) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN a failing refresh WHEN the deadline is still ahead THEN the boundary still fires`() = runTest {
        // Arrange
        val beforeChange = overview(PolymarketSessionKeyStatus.ACTIVE, nextChangeAt = NOW.plus(Duration.ofMinutes(10)))
        val afterChange = overview(PolymarketSessionKeyStatus.ACTIVE)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(
            beforeChange.right(),
            PolymarketWalletError.Network.left(),
            afterChange.right(),
        )
        val service = service()

        service.observe(USER_WALLET_ID).test {
            // Act
            assertThat(awaitItem()).isEqualTo(beforeChange.right())
            advanceTimeBy(Duration.ofMinutes(1).toMillis())
            runCurrent()
            service.refresh(USER_WALLET_ID)
            runCurrent()
            advanceTimeBy(Duration.ofMinutes(9).toMillis())
            runCurrent()

            // Assert
            assertThat(awaitItem()).isEqualTo(afterChange.right())
            coVerify(exactly = 3) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN a next change that has already passed WHEN idle THEN the wallet is reconciled at once`() = runTest {
        // Arrange
        val stale = overview(PolymarketSessionKeyStatus.ACTIVE, nextChangeAt = NOW.minus(Duration.ofMinutes(1)))
        val fresh = overview(PolymarketSessionKeyStatus.ACTIVE, isTradable = true)
        coEvery { reconcile(USER_WALLET_ID) } returnsMany listOf(stale.right(), fresh.right())

        // Act
        service().observe(USER_WALLET_ID).test {
            // Assert
            assertThat(awaitItem()).isEqualTo(stale.right())
            assertThat(awaitItem()).isEqualTo(fresh.right())
            assertThat(testScheduler.currentTime).isEqualTo(0)
            coVerify(exactly = 2) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN reconcile leaks a foreign cancellation WHEN observed THEN the loop restarts`() = runTest {
        // Arrange
        val active = overview(PolymarketSessionKeyStatus.ACTIVE)
        var reconciles = 0
        coEvery { reconcile(USER_WALLET_ID) } coAnswers {
            reconciles++
            if (reconciles == 1) throw CancellationException("timed out elsewhere") else active.right()
        }

        // Act
        service().observe(USER_WALLET_ID).test {
            // Assert
            assertThat(awaitItem()).isEqualTo(active.right())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN reconcile keeps throwing WHEN observed THEN restarts back off`() = runTest {
        // Arrange
        val active = overview(PolymarketSessionKeyStatus.ACTIVE)
        var reconciles = 0
        coEvery { reconcile(USER_WALLET_ID) } coAnswers {
            reconciles++
            if (reconciles <= 3) error("keystore unavailable") else active.right()
        }
        val service = service()
        backgroundScope.launch { service.observe(USER_WALLET_ID).collect() }
        runCurrent()

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()
        val afterFirstDelay = reconciles
        advanceTimeBy(POLL_INTERVAL_MILLIS * 2 - 1)
        runCurrent()
        val beforeSecondDelayEnds = reconciles
        advanceTimeBy(1)
        runCurrent()
        val afterSecondDelay = reconciles
        advanceTimeBy(POLL_INTERVAL_MILLIS * 4)
        runCurrent()

        // Assert
        assertThat(afterFirstDelay).isEqualTo(2)
        assertThat(beforeSecondDelayEnds).isEqualTo(2)
        assertThat(afterSecondDelay).isEqualTo(3)
        assertThat(reconciles).isEqualTo(4)
        assertThat(service.observe(USER_WALLET_ID).first()).isEqualTo(active.right())
    }

    @Test
    fun `GIVEN an unobserved wallet WHEN refresh THEN a later observe reconciles it once`() = runTest {
        // Arrange
        coEvery { reconcile(USER_WALLET_ID) } returns overview(PolymarketSessionKeyStatus.ACTIVE).right()
        val service = service()

        // Act
        service.refresh(USER_WALLET_ID)
        runCurrent()

        // Assert
        coVerify(exactly = 0) { reconcile(any()) }
        service.observe(USER_WALLET_ID).test {
            awaitItem()
            runCurrent()
            expectNoEvents()
            coVerify(exactly = 1) { reconcile(USER_WALLET_ID) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TestScope.service() = PolymarketSessionKeyStateService(
        reconcile = reconcile,
        scope = backgroundScope,
        clock = SchedulerClock(testScheduler),
    )

    private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock() {
        override fun instant(): Instant = NOW.plusMillis(scheduler.currentTime)
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
        const val POLL_INTERVAL_MILLIS = 3_000L
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)

        fun overview(
            status: PolymarketSessionKeyStatus,
            isTradable: Boolean = false,
            nextChangeAt: Instant? = null,
        ): PolymarketSessionKeyOverview {
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
                    nextChangeAt = nextChangeAt,
                ),
                isTradable = isTradable,
            )
        }
    }
}