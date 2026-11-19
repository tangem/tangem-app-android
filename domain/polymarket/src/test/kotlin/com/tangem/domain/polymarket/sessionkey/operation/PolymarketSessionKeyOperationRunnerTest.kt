package com.tangem.domain.polymarket.sessionkey.operation

import com.tangem.domain.polymarket.sessionkey.EXPIRED_DEADLINE
import com.tangem.domain.polymarket.sessionkey.FUTURE_DEADLINE
import com.tangem.domain.polymarket.sessionkey.FakePolymarketPendingAuthorizationStore
import com.tangem.domain.polymarket.sessionkey.createPendingAuthorization
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
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerStore
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorization
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
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSessionKeyOperationRunnerTest {

    private val manage: ManagePolymarketSessionKeyInteractor = mockk()
    private val stateService: PolymarketSessionKeyStateService = mockk()
    private val sessionKeyStore: PolymarketSessionKeyStore = mockk()
    private val bannerStore: PolymarketFastTradingBannerStore = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(manage, stateService, sessionKeyStore, bannerStore)
        coEvery { bannerStore.clear(any()) } just Runs
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
            val outcome = PolymarketSessionKeyOperationOutcome(
                id = 1,
                request = PolymarketSessionKeyOperationRequest.Revoke(FOREIGN),
                result = key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right(),
            )
            assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(
                PolymarketSessionKeyOperationState(running = null, finished = outcome, lastOutcome = outcome),
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
            val error = PolymarketSessionKeyLifecycleError.RevocationFailed(key(FOREIGN, PolymarketSessionKeyStatus.ACTIVE))
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
        fun `GIVEN activation leaves the key active and unlisted WHEN finished THEN the outcome is the active key`() =
            runTest {
                // Arrange
                every { manage.enable(USER_WALLET_ID) } returns flowOf(
                    PolymarketSessionKeyProgress.Signing,
                    PolymarketSessionKeyProgress.Authorizing(key(OWN, PolymarketSessionKeyStatus.PENDING)),
                    PolymarketSessionKeyProgress.CheckingReadiness(key(OWN)),
                    PolymarketSessionKeyProgress.Finished(key(OWN).right()),
                )

                // Act
                val actual = finishedResult(PolymarketSessionKeyOperationRequest.Activate)

                // Assert
                assertThat(actual).isEqualTo(key(OWN).right())
            }

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
    inner class BannerDismissals {

        @Test
        fun `GIVEN activate succeeds WHEN finished THEN the banner dismissals are cleared`() = runTest {
            // Arrange
            every { manage.enable(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )

            // Act
            finishedResult(PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            coVerify(exactly = 1) { bannerStore.clear(USER_WALLET_ID) }
        }

        @Test
        fun `GIVEN reactivate succeeds WHEN finished THEN the banner dismissals are cleared`() = runTest {
            // Arrange
            every { manage.enable(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )

            // Act
            finishedResult(PolymarketSessionKeyOperationRequest.Reactivate)

            // Assert
            coVerify(exactly = 1) { bannerStore.clear(USER_WALLET_ID) }
        }

        @Test
        fun `GIVEN extend succeeds WHEN finished THEN the banner dismissals are cleared`() = runTest {
            // Arrange
            every { manage.renew(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )

            // Act
            finishedResult(PolymarketSessionKeyOperationRequest.Extend)

            // Assert
            coVerify(exactly = 1) { bannerStore.clear(USER_WALLET_ID) }
        }

        @Test
        fun `GIVEN activate fails WHEN finished THEN the banner dismissals are kept`() = runTest {
            // Arrange
            every { manage.enable(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyBusy).left(),
                ),
            )

            // Act
            finishedResult(PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            coVerify(exactly = 0) { bannerStore.clear(any()) }
        }

        @Test
        fun `GIVEN revoke succeeds WHEN finished THEN the banner dismissals are kept`() = runTest {
            // Arrange
            every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right()),
            )

            // Act
            finishedResult(PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))

            // Assert
            coVerify(exactly = 0) { bannerStore.clear(any()) }
        }
    }

    @Nested
    inner class Acknowledge {

        @Test
        fun `GIVEN a finished outcome WHEN acknowledged with its id THEN finished is cleared and last outcome kept`() =
            runTest {
                // Arrange
                val runner = runnerWithFinishedRevocation()

                // Act
                runner.acknowledge(USER_WALLET_ID, operationId = 1)

                // Assert
                assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(
                    PolymarketSessionKeyOperationState(
                        running = null,
                        finished = null,
                        lastOutcome = PolymarketSessionKeyOperationOutcome(
                            id = 1,
                            request = PolymarketSessionKeyOperationRequest.Revoke(FOREIGN),
                            result = key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right(),
                        ),
                    ),
                )
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

        @Test
        fun `GIVEN an acknowledged outcome WHEN a new operation starts THEN last outcome is cleared`() = runTest {
            // Arrange
            val runner = runnerWithFinishedRevocation()
            runner.acknowledge(USER_WALLET_ID, operationId = 1)
            every { manage.enable(USER_WALLET_ID) } returns flow { awaitCancellation() }

            // Act
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(runner.observe(USER_WALLET_ID).value.lastOutcome).isNull()
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

    @Nested
    inner class Resume {

        @Test
        fun `GIVEN nothing is stored WHEN resume THEN nothing is resumed`() = runTest {
            // Arrange
            val runner = runner()

            // Act
            runner.resume(USER_WALLET_ID)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { manage.resume(any()) }
            assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(PolymarketSessionKeyOperationState.IDLE)
        }

        @Test
        fun `GIVEN an expired record WHEN resume THEN it is cleared and no operation starts`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pending(deadline = EXPIRED_DEADLINE))
            val runner = runner()

            // Act
            runner.resume(USER_WALLET_ID)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { manage.resume(any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(PolymarketSessionKeyOperationState.IDLE)
        }

        @Test
        fun `GIVEN a record expiring within the send margin WHEN resume THEN it is kept and no operation starts`() =
            runTest {
                // Arrange
                val deadline = NOW.plusSeconds(MARGIN_SECONDS_LEFT).epochSecond.toString()
                pendingAuthorizations.store(USER_WALLET_ID, pending(deadline = deadline))
                val runner = runner(clock = Clock.fixed(NOW, ZoneOffset.UTC))

                // Act
                runner.resume(USER_WALLET_ID)
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) { manage.resume(any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending(deadline = deadline))
                assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(PolymarketSessionKeyOperationState.IDLE)
            }

        @Test
        fun `GIVEN a record 40 s before its deadline WHEN resume THEN it is resumed`() = runTest {
            // Arrange
            val deadline = NOW.plusSeconds(OUTSIDE_MARGIN_SECONDS_LEFT).epochSecond.toString()
            pendingAuthorizations.store(USER_WALLET_ID, pending(deadline = deadline))
            every { manage.resume(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )
            val runner = runner(clock = Clock.fixed(NOW, ZoneOffset.UTC))

            // Act
            runner.resume(USER_WALLET_ID)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
        }

        @Test
        fun `GIVEN an executable record WHEN resume THEN an extension finishes with the resumed result`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pending())
            every { manage.resume(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(key(OWN).right()),
            )
            val runner = runner()

            // Act
            runner.resume(USER_WALLET_ID)
            advanceUntilIdle()

            // Assert
            val outcome = PolymarketSessionKeyOperationOutcome(
                id = 1,
                request = PolymarketSessionKeyOperationRequest.Extend,
                result = key(OWN).right(),
            )
            assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(
                PolymarketSessionKeyOperationState(running = null, finished = outcome, lastOutcome = outcome),
            )
            verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
        }

        @Test
        fun `GIVEN the resume keeps failing with the record stored WHEN resume THEN it is retried after 10, 20 and 40 s`() =
            runTest {
                // Arrange
                pendingAuthorizations.store(USER_WALLET_ID, pending())
                val failure = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network)
                val callTimes = mutableListOf<Long>()
                every { manage.resume(USER_WALLET_ID) } answers {
                    callTimes += testScheduler.currentTime
                    flowOf(PolymarketSessionKeyProgress.Finished(failure.left()))
                }
                val runner = runner()

                // Act
                runner.resume(USER_WALLET_ID)
                advanceUntilIdle()

                // Assert
                assertThat(callTimes).containsExactly(0L, 10_000L, 30_000L, 70_000L).inOrder()
                assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(
                    PolymarketSessionKeyOperationFailure(error = failure, isAfterRevocation = false).left(),
                )
            }

        @Test
        fun `GIVEN the record is gone after a failure WHEN resume THEN it is not retried`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pending())
            val failure = PolymarketSessionKeyLifecycleError.StaleSignature(cause = null)
            every { manage.resume(USER_WALLET_ID) } returns flow {
                pendingAuthorizations.clear(USER_WALLET_ID)
                emit(PolymarketSessionKeyProgress.Finished(failure.left()))
            }
            val runner = runner()

            // Act
            runner.resume(USER_WALLET_ID)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
            assertThat(testScheduler.currentTime).isEqualTo(0L)
            assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(
                PolymarketSessionKeyOperationFailure(error = failure, isAfterRevocation = false).left(),
            )
        }

        @Test
        fun `GIVEN the resume ends unsettled past the deadline WHEN resume THEN it is not retried`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pending())
            val failure = PolymarketSessionKeyLifecycleError.Unsettled(key(OWN, PolymarketSessionKeyStatus.REVOKED))
            every { manage.resume(USER_WALLET_ID) } returns flow {
                pendingAuthorizations.store(USER_WALLET_ID, pending(deadline = EXPIRED_DEADLINE))
                emit(PolymarketSessionKeyProgress.Finished(failure.left()))
            }
            val runner = runner()

            // Act
            runner.resume(USER_WALLET_ID)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
            assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(
                PolymarketSessionKeyOperationFailure(error = failure, isAfterRevocation = false).left(),
            )
        }

        @Test
        fun `GIVEN extend fails keeping a resumable record WHEN finished THEN the authorization is resumed`() =
            runTest {
                // Arrange
                val failure = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network)
                every { manage.renew(USER_WALLET_ID) } returns flow {
                    pendingAuthorizations.store(USER_WALLET_ID, pending())
                    emit(PolymarketSessionKeyProgress.Finished(failure.left()))
                }
                every { manage.resume(USER_WALLET_ID) } returns flowOf(
                    PolymarketSessionKeyProgress.Finished(key(OWN).right()),
                )
                val runner = runner()

                // Act
                runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Extend)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
                assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(key(OWN).right())
            }

        @Test
        fun `GIVEN revoke fails while a resumable record waits WHEN finished THEN nothing is resumed`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pending())
            val failure = PolymarketSessionKeyLifecycleError.OperationInProgress
            every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(failure.left()),
            )
            val runner = runner()

            // Act
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { manage.resume(any()) }
            assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(
                PolymarketSessionKeyOperationFailure(error = failure, isAfterRevocation = false).left(),
            )
        }

        @Test
        fun `GIVEN the record leaves the resumable window during a retry pause WHEN resume THEN it is not retried`() =
            runTest {
                // Arrange
                val deadline = NOW.plusSeconds(OUTSIDE_MARGIN_SECONDS_LEFT).epochSecond.toString()
                pendingAuthorizations.store(USER_WALLET_ID, pending(deadline = deadline))
                val failure = PolymarketSessionKeyLifecycleError.Unsettled(key(OWN, PolymarketSessionKeyStatus.REVOKING))
                every { manage.resume(USER_WALLET_ID) } returns flowOf(
                    PolymarketSessionKeyProgress.Finished(failure.left()),
                )
                val runner = runner(clock = SchedulerClock(testScheduler))

                // Act
                runner.resume(USER_WALLET_ID)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
                assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(
                    PolymarketSessionKeyOperationFailure(error = failure, isAfterRevocation = false).left(),
                )
            }

        @Test
        fun `GIVEN extend fails while the record cannot be read WHEN finished THEN nothing is resumed`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pending())
            pendingAuthorizations.readFailure = PolymarketSessionKeyError.Failure(IllegalStateException("locked"))
            val failure = PolymarketSessionKeyLifecycleError.OperationInProgress
            every { manage.renew(USER_WALLET_ID) } returns flowOf(PolymarketSessionKeyProgress.Finished(failure.left()))
            val runner = runner()

            // Act
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Extend)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { manage.resume(any()) }
            assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(
                PolymarketSessionKeyOperationFailure(error = failure, isAfterRevocation = false).left(),
            )
        }

        @Test
        fun `GIVEN an outcome the host has not acknowledged WHEN resume THEN it is kept and nothing is resumed`() =
            runTest {
                // Arrange
                val failure = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network)
                every { manage.enable(USER_WALLET_ID) } returns flowOf(
                    PolymarketSessionKeyProgress.Finished(failure.left()),
                )
                val runner = runner()
                runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Reactivate)
                advanceUntilIdle()
                val finished = runner.observe(USER_WALLET_ID).value.finished
                pendingAuthorizations.store(USER_WALLET_ID, pending())

                // Act
                runner.resume(USER_WALLET_ID)
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) { manage.resume(any()) }
                assertThat(runner.observe(USER_WALLET_ID).value.finished).isEqualTo(finished)
            }

        @Test
        fun `GIVEN a resume refused for an unacknowledged outcome WHEN acknowledged THEN the pair is resumed`() =
            runTest {
                // Arrange
                val failure = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network)
                every { manage.enable(USER_WALLET_ID) } returns flowOf(
                    PolymarketSessionKeyProgress.Finished(failure.left()),
                )
                every { manage.resume(USER_WALLET_ID) } returns flowOf(
                    PolymarketSessionKeyProgress.Finished(key(OWN).right()),
                )
                val runner = runner()
                runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Reactivate)
                advanceUntilIdle()
                val finishedId = requireNotNull(runner.observe(USER_WALLET_ID).value.finished).id
                pendingAuthorizations.store(USER_WALLET_ID, pending())
                runner.resume(USER_WALLET_ID)
                advanceUntilIdle()

                // Act
                runner.acknowledge(USER_WALLET_ID, finishedId)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
                assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(key(OWN).right())
            }

        @Test
        fun `GIVEN no resume was refused WHEN an outcome is acknowledged THEN nothing is resumed`() = runTest {
            // Arrange
            val failure = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network)
            every { manage.enable(USER_WALLET_ID) } returns flowOf(
                PolymarketSessionKeyProgress.Finished(failure.left()),
            )
            val runner = runner()
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Reactivate)
            advanceUntilIdle()
            val finishedId = requireNotNull(runner.observe(USER_WALLET_ID).value.finished).id
            pendingAuthorizations.store(USER_WALLET_ID, pending())

            // Act
            runner.acknowledge(USER_WALLET_ID, finishedId)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { manage.resume(any()) }
        }

        @Test
        fun `GIVEN the record cannot be read WHEN resume THEN no operation starts and the record is kept`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pending())
            pendingAuthorizations.readFailure = PolymarketSessionKeyError.Failure(IllegalStateException("locked"))
            val runner = runner()

            // Act
            runner.resume(USER_WALLET_ID)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { manage.resume(any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending())
            assertThat(runner.observe(USER_WALLET_ID).value).isEqualTo(PolymarketSessionKeyOperationState.IDLE)
        }

        @Test
        fun `GIVEN extend fails without a record WHEN finished THEN nothing is resumed`() = runTest {
            // Arrange
            val failure = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network)
            every { manage.renew(USER_WALLET_ID) } returns flowOf(PolymarketSessionKeyProgress.Finished(failure.left()))
            val runner = runner()

            // Act
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Extend)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { manage.resume(any()) }
            assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(
                PolymarketSessionKeyOperationFailure(error = failure, isAfterRevocation = false).left(),
            )
        }

        @Test
        fun `GIVEN an operation runs WHEN resume THEN it is ignored`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pending())
            every { manage.enable(USER_WALLET_ID) } returns flow { awaitCancellation() }
            val runner = runner()
            runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate)
            runCurrent()

            // Act
            runner.resume(USER_WALLET_ID)
            runCurrent()

            // Assert
            verify(exactly = 0) { manage.resume(any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending())
        }

        @Test
        fun `GIVEN a resume refused while a revoke runs WHEN the revoke is acknowledged THEN the pair is resumed once`() =
            runTest {
                // Arrange
                val revocation = CompletableDeferred<Unit>()
                every { manage.revoke(USER_WALLET_ID, FOREIGN) } returns flow {
                    revocation.await()
                    emit(PolymarketSessionKeyProgress.Finished(key(FOREIGN, PolymarketSessionKeyStatus.REVOKED).right()))
                }
                every { manage.resume(USER_WALLET_ID) } returns flowOf(
                    PolymarketSessionKeyProgress.Finished(key(OWN).right()),
                )
                val runner = runner()
                runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Revoke(FOREIGN))
                runCurrent()
                pendingAuthorizations.store(USER_WALLET_ID, pending())
                runner.resume(USER_WALLET_ID)
                runCurrent()
                revocation.complete(Unit)
                advanceUntilIdle()
                val revokeId = requireNotNull(runner.observe(USER_WALLET_ID).value.finished).id

                // Act
                runner.acknowledge(USER_WALLET_ID, revokeId)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
                assertThat(runner.observe(USER_WALLET_ID).value.finished?.result).isEqualTo(key(OWN).right())
            }

        @Test
        fun `GIVEN a resume reads the record WHEN activation starts THEN it is busy and the resume runs`() =
            runTest {
                // Arrange
                pendingAuthorizations.store(USER_WALLET_ID, pending())
                every { manage.resume(USER_WALLET_ID) } returns flow { awaitCancellation() }
                val runner = runner()
                runner.resume(USER_WALLET_ID)

                // Act
                val start = runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate)
                runCurrent()

                // Assert
                assertThat(start).isEqualTo(PolymarketSessionKeyOperationStart.Busy)
                verify(exactly = 0) { manage.enable(any()) }
                verify(exactly = 1) { manage.resume(USER_WALLET_ID) }
            }

        @Test
        fun `GIVEN nothing is stored WHEN resume finished reading THEN the user can start an activation`() = runTest {
            // Arrange
            every { manage.enable(USER_WALLET_ID) } returns flow { awaitCancellation() }
            val runner = runner()
            runner.resume(USER_WALLET_ID)
            runCurrent()

            // Act
            val start = runner.start(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate)

            // Assert
            assertThat(start).isEqualTo(PolymarketSessionKeyOperationStart.Started(id = 1))
        }

        @Test
        fun `GIVEN a replacement record WHEN resume THEN it runs as an activation of the authorized key`() = runTest {
            // Arrange
            val replacement = createPendingAuthorization(purpose = PolymarketPendingAuthorization.Purpose.REPLACE)
            pendingAuthorizations.store(USER_WALLET_ID, replacement)
            every { manage.resume(USER_WALLET_ID) } returns flow { awaitCancellation() }
            val runner = runner()

            // Act
            runner.resume(USER_WALLET_ID)
            runCurrent()

            // Assert
            assertThat(runner.observe(USER_WALLET_ID).value.running).isEqualTo(
                PolymarketSessionKeyOperation(
                    id = 1,
                    request = PolymarketSessionKeyOperationRequest.Activate,
                    targetAddress = replacement.authorization.sessionKeyAddress,
                    stage = PolymarketSessionKeyOperationStage.SIGNING,
                ),
            )
        }

        private fun pending(deadline: String = FUTURE_DEADLINE) = createPendingAuthorization(deadline)
    }

    private val pendingAuthorizations = FakePolymarketPendingAuthorizationStore()

    private fun TestScope.runner(clock: Clock = Clock.systemUTC()) = PolymarketSessionKeyOperationRunner(
        manage = manage,
        stateService = stateService,
        sessionKeyStore = sessionKeyStore,
        bannerStore = bannerStore,
        pendingAuthorizations = pendingAuthorizations,
        appScope = TestAppCoroutineScope(coroutineContext = StandardTestDispatcher(testScheduler)),
        clock = clock,
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
        const val MARGIN_SECONDS_LEFT = 20L
        const val OUTSIDE_MARGIN_SECONDS_LEFT = 40L

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
            deviceId = "this-device",
            assessment = PolymarketSessionKeyAssessment(
                deviceState = deviceState,
                reconciliation = PolymarketSessionKeyReconciliation.Unavailable,
                nextChangeAt = null,
            ),
            isTradable = false,
        )
    }

    private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock() {

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this

        override fun instant(): Instant = NOW.plusMillis(scheduler.currentTime)
    }
}