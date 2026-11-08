package com.tangem.domain.polymarket.sessionkey.operation

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.ManagePolymarketSessionKeyInteractor
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessment
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSessionKeyOperationRunnerTest {

    private val manage: ManagePolymarketSessionKeyInteractor = mockk()
    private val stateService: PolymarketSessionKeyStateService = mockk()
    private val sessionKeyStore: PolymarketSessionKeyStore = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(manage, stateService, sessionKeyStore)
        every { stateService.refresh(any()) } just Runs
        coEvery { stateService.refreshAndAwait(any(), any()) } just Runs
        coEvery { sessionKeyStore.heldAddress(any()) } returns OWN
        givenDeviceState(PolymarketSessionKeyDeviceState.OfferEnable)
    }

    @Nested
    inner class Start {

        @Test
        fun `GIVEN no operation WHEN start activate THEN running signing is published`() = runTest {
            // Arrange
            every { manage.enable(USER_WALLET_ID) } returns flow { awaitCancellation() }
            val runner = runner()

            // Act
            val start = runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(start).isEqualTo(PolymarketSessionKeyOperationStart.Started(id = 1))
            assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(
                PolymarketSessionKeyOperationState(
                    running = PolymarketSessionKeyOperation(
                        id = 1,
                        request = PolymarketSessionKeyOperationRequest.Activate,
                        targetAddress = null,
                        stage = PolymarketSessionKeyOperationStage.SIGNING,
                    ),
                    finished = null,
                ),
            )
        }

        @Test
        fun `GIVEN an operation runs WHEN start another THEN busy is returned and nothing is called`() = runTest {
            // Arrange
            every { manage.enable(USER_WALLET_ID) } returns flow { awaitCancellation() }
            val runner = runner()
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate)
            runCurrent()

            // Act
            val revoke = runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
            val toggle = runner.start(
                USER_WALLET_ID,
                PolymarketSessionKeyOperationRequest.SetEnabled(OWN, isEnabled = false),
            )
            runCurrent()

            // Assert
            assertThat(revoke).isEqualTo(PolymarketSessionKeyOperationStart.Busy)
            assertThat(toggle).isEqualTo(PolymarketSessionKeyOperationStart.Busy)
            verify(exactly = 0) { manage.revoke(any(), any()) }
            coVerify(exactly = 0) { manage.setEnabled(any(), any(), any()) }
        }

        @Test
        fun `GIVEN two wallets WHEN both start THEN both run`() = runTest {
            // Arrange
            every { manage.revoke(any(), FOREIGN) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right()),
            )
            val runner = runner()

            // Act
            val first = runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
            val second = runner.start(OTHER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
            advanceUntilIdle()

            // Assert
            assertThat(first).isInstanceOf(PolymarketSessionKeyOperationStart.Started::class.java)
            assertThat(second).isInstanceOf(PolymarketSessionKeyOperationStart.Started::class.java)
            verify(exactly = 1) { manage.revoke(USER_WALLET_ID, FOREIGN) }
            verify(exactly = 1) { manage.revoke(OTHER_WALLET_ID, FOREIGN) }
        }

        @Test
        fun `GIVEN an operation finished WHEN start another THEN it runs`() = runTest {
            // Arrange
            every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right()),
            )
            val runner = runner()
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
            advanceUntilIdle()

            // Act
            val start = runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
            advanceUntilIdle()

            // Assert
            assertThat(start).isEqualTo(PolymarketSessionKeyOperationStart.Started(id = 2))
            verify(exactly = 2) { manage.revoke(USER_WALLET_ID, FOREIGN) }
        }
    }

    @Nested
    inner class Progress {

        @Test
        fun `GIVEN enable emits progress WHEN collected THEN stages follow signing authorizing checking readiness`() =
            runTest {
                // Arrange
                val gates = List(3) { CompletableDeferred<Unit>() }
                every { manage.enable(USER_WALLET_ID) } returns flow {
                    emit(PolymarketSessionKeyProgress.Signing)
                    gates[0].await()
                    emit(PolymarketSessionKeyProgress.Authorizing(key(OWN, PolymarketSessionKeyStatus.PENDING)))
                    gates[1].await()
                    emit(PolymarketSessionKeyProgress.CheckingReadiness(key(OWN)))
                    gates[2].await()
                    emit(PolymarketSessionKeyProgress.Finished(key(OWN).right()))
                }
                val runner = runner()
                givenDeviceState(PolymarketSessionKeyDeviceState.OfferEnable)
                coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns null
                val stages = mutableListOf<Pair<PolymarketSessionKeyOperationStage, String?>>()

                // Act
                runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate)
                gates.forEach { gate ->
                    runCurrent()
                    runner.observe(USER_WALLET_ID).value.running?.let { stages += it.stage to it.targetAddress }
                    gate.complete(Unit)
                }
                advanceUntilIdle()

                // Assert
                assertThat(stages).containsExactly(
                    PolymarketSessionKeyOperationStage.SIGNING to null,
                    PolymarketSessionKeyOperationStage.AUTHORIZING to OWN,
                    PolymarketSessionKeyOperationStage.CHECKING_READINESS to OWN,
                ).inOrder()
                verify(exactly = 1) { stateService.refresh(USER_WALLET_ID) }
            }

        @Test
        fun `GIVEN extend WHEN started THEN target is the held address`() = runTest {
            // Arrange
            every { manage.renew(USER_WALLET_ID) } returns flow { awaitCancellation() }
            val runner = runner()

            // Act
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Extend)
            runCurrent()

            // Assert
            assertThat(runner.observe(USER_WALLET_ID).value.running?.targetAddress).isEqualTo(OWN)
        }

        @Test
        fun `GIVEN the observer is gone WHEN the operation runs THEN it completes in the app scope`() = runTest {
            // Arrange
            every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flowOf(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(FOREIGN, PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right()),
            )
            val runner = runner()

            // Act
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
            advanceUntilIdle()

            // Assert
            assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(
                PolymarketSessionKeyOperationState(
                    running = null,
                    finished = PolymarketSessionKeyOperationOutcome(
                        id = 1,
                        request = PolymarketSessionKeyOperationRequest.Revoke(FOREIGN),
                        result = key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right(),
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN the operation finished WHEN the refresh is awaited THEN running is cleared together with finished`() =
            runTest {
                // Arrange
                val refreshed = CompletableDeferred<Unit>()
                coEvery { stateService.refreshAndAwait(USER_WALLET_ID, any()) } coAnswers { refreshed.await() }
                every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flowOf(
                    PolymarketSessionKeyProgress.Finished(key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right()),
                )
                val runner = runner()
                runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
                runCurrent()
                val whileRefreshing = runner.observe(USER_WALLET_ID).value

                // Act
                refreshed.complete(Unit)
                advanceUntilIdle()

                // Assert
                assertThat(whileRefreshing.running).isNotNull()
                assertThat(whileRefreshing.finished).isNull()
                assertThat(runner.observe(USER_WALLET_ID).value.running).isNull()
                assertThat(runner.observe(USER_WALLET_ID).value.finished).isNotNull()
            }

        @Test
        fun `GIVEN the interactor throws WHEN the operation runs THEN it ends in a failure and the slot is free`() =
            runTest {
                // Arrange
                val cause = IllegalStateException("keystore unavailable")
                every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flow { throw cause }
                val runner = runner()

                // Act
                runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
                advanceUntilIdle()

                // Assert
                assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(
                    PolymarketSessionKeyOperationFailure(
                        error = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Failure(cause)),
                        isAfterRevocation = false,
                    ).left(),
                )
                assertThat(runner.observe(USER_WALLET_ID).value.running).isNull()
            }
    }

    @Nested
    inner class Revocation {

        @Test
        fun `GIVEN renew revoked the key WHEN the authorization fails THEN the failure is after revocation`() =
            runTest {
                // Arrange
                val error = PolymarketSessionKeyLifecycleError.StaleSignature(cause = null)
                every { manage.renew(USER_WALLET_ID) } returns flowOf(
                    PolymarketSessionKeyProgress.Signing,
                    PolymarketSessionKeyProgress.Revoking(key(OWN, PolymarketSessionKeyStatus.REVOKING)),
                    PolymarketSessionKeyProgress.Revoked(key(OWN, PolymarketSessionKeyStatus.REVOKED)),
                    PolymarketSessionKeyProgress.Finished(error.left()),
                )

                // Act
                val actual = finishedResult(PolymarketSessionKeyOperationRequest.Extend)

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyOperationFailure(error = error, isAfterRevocation = true).left(),
                )
            }

        @Test
        fun `GIVEN renew fails to revoke WHEN finished THEN the failure is before revocation`() = runTest {
            // Arrange
            val error = PolymarketSessionKeyLifecycleError.RevocationFailed(key(OWN))
            every { manage.renew(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(OWN, PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(error.left()),
            )

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Extend)

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyOperationFailure(error = error, isAfterRevocation = false).left(),
            )
        }

        @Test
        fun `GIVEN replace by revocation revoked WHEN unsettled THEN the failure is after revocation`() = runTest {
            // Arrange
            val replacement = PolymarketSessionKeyDeviceState.Replace.ByRevocation(key(PREVIOUS))
            givenDeviceState(replacement)
            val error = PolymarketSessionKeyLifecycleError.Unsettled(key(OWN, PolymarketSessionKeyStatus.PENDING))
            every { manage.replace(USER_WALLET_ID, replacement) } returns flowOf(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(PREVIOUS, PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Revoked(key(PREVIOUS, PolymarketSessionKeyStatus.REVOKED)),
                PolymarketSessionKeyProgress.Authorizing(key(OWN, PolymarketSessionKeyStatus.PENDING)),
                PolymarketSessionKeyProgress.Finished(error.left()),
            )

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyOperationFailure(error = error, isAfterRevocation = true).left(),
            )
        }

        @Test
        fun `GIVEN revoke fails WHEN finished THEN the failure is not after revocation`() = runTest {
            // Arrange
            val error = PolymarketSessionKeyLifecycleError.RevocationFailed(sessionKey = null)
            every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flowOf(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(FOREIGN, PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(error.left()),
            )

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyOperationFailure(error = error, isAfterRevocation = false).left(),
            )
        }

        @Test
        fun `GIVEN the signing is cancelled WHEN finished THEN the failure is cancelled`() = runTest {
            // Arrange
            every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flowOf(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Failed(
                        PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.UserCancelled),
                    ).left(),
                ),
            )

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))

            // Assert
            assertThat(actual.leftOrNull()?.isCancelled).isTrue()
        }
    }

    @Nested
    inner class Activation {

        @Test
        fun `GIVEN reenable WHEN activate THEN enable is called`() = runTest {
            // Arrange
            givenDeviceState(PolymarketSessionKeyDeviceState.Reenable(key(OWN, PolymarketSessionKeyStatus.EXPIRED)))
            every { manage.enable(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(actual).isEqualTo(key(OWN).right())
            verify(exactly = 0) { manage.renew(any()) }
        }

        @Test
        fun `GIVEN repair by renewal WHEN activate THEN the key is renewed`() = runTest {
            // Arrange
            givenDeviceState(
                PolymarketSessionKeyDeviceState.RepairByRenewal(key(OWN, PolymarketSessionKeyStatus.FAILED)),
            )
            every { manage.renew(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(actual).isEqualTo(key(OWN).right())
            verify(exactly = 0) { manage.enable(any(), any()) }
        }

        @Test
        fun `GIVEN the overview cannot be read WHEN activate THEN enable is called`() = runTest {
            // Arrange
            every { stateService.observe(USER_WALLET_ID) } returns flowOf(PolymarketWalletError.Network.left())
            every { manage.enable(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(actual).isEqualTo(key(OWN).right())
        }

        @Test
        fun `GIVEN the key is still enabling WHEN activate THEN it ends in progress without signing`() = runTest {
            // Arrange
            givenDeviceState(PolymarketSessionKeyDeviceState.Enabling(key(OWN, PolymarketSessionKeyStatus.PENDING)))

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyOperationFailure(
                    error = PolymarketSessionKeyLifecycleError.OperationInProgress,
                    isAfterRevocation = false,
                ).left(),
            )
            verify(exactly = 0) { manage.enable(any(), any()) }
        }

        @Test
        fun `GIVEN reactivate WHEN started THEN enable is called without revocation`() = runTest {
            // Arrange
            every { manage.enable(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.Reactivate)

            // Assert
            assertThat(actual).isEqualTo(key(OWN).right())
            verify(exactly = 0) { manage.renew(any()) }
            verify(exactly = 0) { manage.revoke(any(), any()) }
        }
    }

    @Nested
    inner class Toggle {

        @Test
        fun `GIVEN set enabled succeeds WHEN finished THEN the state is refreshed before the key is returned`() =
            runTest {
                // Arrange
                val paused = key(OWN).copy(isEnabled = false)
                coEvery { manage.setEnabled(USER_WALLET_ID, OWN, false) } returns paused.right()

                // Act
                val actual = finishedResult(PolymarketSessionKeyOperationRequest.SetEnabled(OWN, isEnabled = false))

                // Assert
                assertThat(actual).isEqualTo(paused.right())
                coVerifyOrder {
                    manage.setEnabled(USER_WALLET_ID, OWN, false)
                    stateService.refreshAndAwait(USER_WALLET_ID, any())
                }
            }

        @Test
        fun `GIVEN set enabled fails WHEN finished THEN the failure wraps the session key error`() = runTest {
            // Arrange
            coEvery { manage.setEnabled(USER_WALLET_ID, OWN, true) } returns
                PolymarketSessionKeyError.SessionKeyBusy.left()

            // Act
            val actual = finishedResult(PolymarketSessionKeyOperationRequest.SetEnabled(OWN, isEnabled = true))

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyOperationFailure(
                    error = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyBusy),
                    isAfterRevocation = false,
                ).left(),
            )
        }

        @Test
        fun `GIVEN set enabled WHEN started THEN running is toggling the address`() = runTest {
            // Arrange
            val toggled = CompletableDeferred<Unit>()
            coEvery { manage.setEnabled(USER_WALLET_ID, OWN, false) } coAnswers {
                toggled.await()
                key(OWN).right()
            }
            val runner = runner()

            // Act
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.SetEnabled(OWN, isEnabled = false))
            runCurrent()

            // Assert
            val running = runner.observe(USER_WALLET_ID).value.running
            assertThat(running?.stage).isEqualTo(PolymarketSessionKeyOperationStage.TOGGLING)
            assertThat(running?.targetAddress).isEqualTo(OWN)
            toggled.complete(Unit)
        }
    }

    @Nested
    inner class Acknowledge {

        @Test
        fun `GIVEN a finished outcome WHEN acknowledged with its id THEN finished is cleared`() = runTest {
            // Arrange
            val runner = runnerWithFinishedRevocation()

            // Act
            runner.acknowledge(USER_WALLET_ID, operationId = 1)

            // Assert
            assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(PolymarketSessionKeyOperationState.IDLE)
        }

        @Test
        fun `GIVEN a finished outcome WHEN acknowledged with another id THEN finished is kept`() = runTest {
            // Arrange
            val runner = runnerWithFinishedRevocation()

            // Act
            runner.acknowledge(USER_WALLET_ID, operationId = 2)

            // Assert
            assertThat(runner.observe(USER_WALLET_ID).value.finished?.id).isEqualTo(1)
        }

        @Test
        fun `GIVEN a finished outcome WHEN a new operation starts THEN finished is cleared`() = runTest {
            // Arrange
            val runner = runnerWithFinishedRevocation()
            every { manage.enable(USER_WALLET_ID) } returns flow { awaitCancellation() }

            // Act
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(runner.observe(USER_WALLET_ID).value.finished).isNull()
        }

        private fun TestScope.runnerWithFinishedRevocation(): PolymarketSessionKeyOperationRunner {
            every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right()),
            )
            val runner = runner()
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
            advanceUntilIdle()
            return runner
        }
    }

    private fun TestScope.runner() = PolymarketSessionKeyOperationRunner(
        manage = manage,
        stateService = stateService,
        sessionKeyStore = sessionKeyStore,
        appScope = TestAppCoroutineScope(coroutineContext = StandardTestDispatcher(testScheduler)),
    )

    private fun TestScope.finishedResult(request: PolymarketSessionKeyOperationRequest) = runner().run {
        start(USER_WALLET_ID, request)
        advanceUntilIdle()
        checkNotNull(observe(USER_WALLET_ID).value.finished).result
    }

    private fun givenDeviceState(deviceState: PolymarketSessionKeyDeviceState) {
        every { stateService.observe(USER_WALLET_ID) } returns flowOf(overview(deviceState).right())
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
        const val OWN = "0x5e55000000000000000000000000000000000001"
        const val PREVIOUS = "0x5e55000000000000000000000000000000000002"
        const val FOREIGN = "0x5e55000000000000000000000000000000000003"
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)

        fun key(address: String, status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE) =
            PolymarketSessionKey(
                address = address,
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

        fun overview(deviceState: PolymarketSessionKeyDeviceState) = PolymarketSessionKeyOverview(
            wallet = PolymarketWalletState(
                depositWalletAddress = "0x2222222222222222222222222222222222222222",
                status = PolymarketWalletStatus.READY_TO_TRADE,
                sessionKeys = emptyList(),
            ),
            localKeyAddress = OWN,
            assessment = PolymarketSessionKeyAssessment(
                deviceState = deviceState,
                reconciliation = PolymarketSessionKeyReconciliation.Unavailable,
                nextChangeAt = null,
            ),
            isTradable = false,
        )
    }
}