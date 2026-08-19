package com.tangem.features.polymarket.impl.onboarding.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.interactor.ResolvePolymarketEntryInteractor
import com.tangem.domain.polymarket.interactor.RunPolymarketOnboardingInteractor
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class PolymarketOnboardingModelTest {

    private val resolvePolymarketEntryInteractor: ResolvePolymarketEntryInteractor = mockk()
    private val runOnboardingUseCase: RunPolymarketOnboardingInteractor = mockk()
    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)

    private val userWalletId = UserWalletId("011")
    private val params = PolymarketOnboardingParams(userWalletId = userWalletId)

    @BeforeEach
    fun resetMocks() {
        clearMocks(resolvePolymarketEntryInteractor, runOnboardingUseCase, router, urlOpener)
    }

    @Test
    fun `GIVEN resolution fails WHEN model created THEN the error overlay is raised AND nothing is navigated`() =
        runTest {
            // Arrange
            gateFails(PolymarketOnboardingError.Network)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state.overlay).isInstanceOf(PolymarketOnboardingUM.Overlay.Error::class.java)
            assertThat(state.isStarting).isFalse()
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN entry is Onboarded WHEN model created THEN the feed is opened`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Onboarded)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(
                    PolymarketRoute.Main(userWalletId = userWalletId),
                ),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is Onboard WHEN model created THEN no overlay is shown AND nothing is navigated`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.overlay).isNull()
        assertThat(state.isStarting).isFalse()
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the error overlay is raised WHEN retry is tapped THEN the entry is resolved again`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) } returnsMany listOf(
            PolymarketOnboardingError.Network.left(),
            PolymarketEntry.Onboarded.right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value.overlay as PolymarketOnboardingUM.Overlay.Error).onRetryClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) }
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(
                    PolymarketRoute.Main(userWalletId = userWalletId),
                ),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN retry is tapped twice WHEN the superseded attempt fails THEN the fresh resolution survives`() =
        runTest {
            // Arrange
            var attempt = 0
            coEvery { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) } coAnswers {
                when (++attempt) {
                    1 -> PolymarketOnboardingError.Network.left()
                    2 -> {
                        runCatching { delay(SUPERSEDED_ATTEMPT_DELAY_MILLIS) }
                        PolymarketOnboardingError.Network.left()
                    }
                    else -> PolymarketEntry.Onboarded.right()
                }
            }
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val overlay = model.uiState.value.overlay as PolymarketOnboardingUM.Overlay.Error

            // Act
            overlay.onRetryClick()
            runCurrent()
            overlay.onRetryClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.isStarting).isTrue()
            assertThat(model.uiState.value.overlay).isNull()
            verify(exactly = 1) {
                router.replaceAll(
                    routes = arrayOf(
                        PolymarketRoute.Main(userWalletId = userWalletId),
                    ),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GateAsksForNothing {

        @Test
        fun `GIVEN the key is not derived here WHEN the gate opens THEN nothing is derived AND the button is idle`() =
            runTest {
                // Arrange
                gateResolves(PolymarketEntry.Undetermined)

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                val state = model.uiState.value
                assertThat(state.isStarting).isFalse()
                assertThat(state.overlay).isNull()
                assertThat(state.startButtonText)
                    .isEqualTo(resourceReference(R.string.prediction_onboarding_start_button))
                coVerify(exactly = 0) { resolvePolymarketEntryInteractor(userWalletId) }
                verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN an undetermined entry WHEN start is pressed THEN the wallet turns out onboarded AND opens feed`() =
            runTest {
                // Arrange
                gateResolves(PolymarketEntry.Undetermined)
                startResolves(PolymarketEntry.Onboarded)
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.uiState.value.onStartClick()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    router.replaceAll(
                        routes = arrayOf(
                            PolymarketRoute.Main(userWalletId = userWalletId),
                        ),
                        onComplete = any(),
                    )
                }
                verify(exactly = 0) { runOnboardingUseCase(userWalletId) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN an undetermined entry WHEN start resolves to onboarding owed THEN the run is launched`() = runTest {
            // Arrange
            gateResolves(PolymarketEntry.Undetermined)
            startResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))
            every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Deriving)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.uiState.value.onStartClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { runOnboardingUseCase(userWalletId) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN start is pressed WHEN the card tap is cancelled THEN the button returns to idle AND no overlay`() =
            runTest {
                // Arrange
                gateResolves(PolymarketEntry.Undetermined)
                coEvery { resolvePolymarketEntryInteractor(userWalletId) } returns
                    PolymarketOnboardingError.Derivation(PolymarketDerivationError.UserCancelled).left()
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.uiState.value.onStartClick()
                advanceUntilIdle()

                // Assert
                val state = model.uiState.value
                assertThat(state.isStarting).isFalse()
                assertThat(state.overlay).isNull()
                verify(exactly = 0) { runOnboardingUseCase(userWalletId) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the run refuses the region WHEN start is pressed THEN the sheet overlay is raised`() = runTest {
            // Arrange
            owesOnboarding()
            every { runOnboardingUseCase(userWalletId) } returns flowOf(
                PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.RegionBlocked,
                    isRetryable = false,
                ),
            )
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.uiState.value.onStartClick()
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state.overlay).isInstanceOf(PolymarketOnboardingUM.Overlay.RegionRestrictions::class.java)
            assertThat(state.isStarting).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the region overlay is raised WHEN dismissed THEN it closes AND nothing is navigated`() = runTest {
            // Arrange
            owesOnboarding()
            every { runOnboardingUseCase(userWalletId) } returns flowOf(
                PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.RegionBlocked,
                    isRetryable = false,
                ),
            )
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.uiState.value.onStartClick()
            advanceUntilIdle()

            // Act
            (model.uiState.value.overlay as PolymarketOnboardingUM.Overlay.RegionRestrictions).onDismiss()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.overlay).isNull()
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }
    }

    @Test
    fun `GIVEN entry owes onboarding WHEN start clicked THEN run is launched`() = runTest {
        // Arrange
        owesOnboarding()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Deriving)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.onStartClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { runOnboardingUseCase(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no account yet WHEN the entry resolves THEN the button invites the user to start`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.startButtonText)
            .isEqualTo(resourceReference(R.string.prediction_onboarding_start_button))
        model.onDestroy()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ProgressMapping {

        private fun provideTestModels() = listOf(
            ProgressModel(progress = PolymarketOnboardingProgress.Deriving, expectedIsStarting = true),
            ProgressModel(progress = PolymarketOnboardingProgress.AwaitingSignature, expectedIsStarting = true),
            ProgressModel(
                progress = PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
                expectedIsStarting = true,
            ),
            ProgressModel(
                progress = PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.Network,
                    isRetryable = true,
                ),
                expectedIsStarting = false,
            ),
            ProgressModel(
                progress = PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.AddressMismatch(expected = "0xA", actual = "0xB"),
                    isRetryable = false,
                ),
                expectedIsStarting = false,
            ),
        )

        @ParameterizedTest
        @ProvideTestModels
        fun progressMapping(model: ProgressModel) = runTest {
            // Arrange
            owesOnboarding()
            every { runOnboardingUseCase(userWalletId) } returns flowOf(model.progress)
            val subject = createModel(testScope = this)
            advanceUntilIdle()
            val idleState = subject.uiState.value

            // Act
            idleState.onStartClick()
            advanceUntilIdle()

            // Assert
            assertThat(subject.uiState.value).isEqualTo(idleState.copy(isStarting = model.expectedIsStarting))
            subject.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResumeLabel {

        private fun provideTestModels() = listOf(
            ResumeLabelModel(status = PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            ResumeLabelModel(status = PolymarketWalletStatus.DEPLOYED),
            ResumeLabelModel(status = PolymarketWalletStatus.DEPLOYMENT_FAILED),
            ResumeLabelModel(status = PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            ResumeLabelModel(status = PolymarketWalletStatus.APPROVALS_FAILED),
            ResumeLabelModel(status = PolymarketWalletStatus.UNKNOWN),
        )

        @ParameterizedTest
        @ProvideTestModels
        fun resumeLabel(model: ResumeLabelModel) = runTest {
            // Arrange
            gateResolves(PolymarketEntry.Onboard(status = model.status))

            // Act
            val subject = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(subject.uiState.value.startButtonText)
                .isEqualTo(resourceReference(R.string.common_continue))
            subject.onDestroy()
        }
    }

    @Test
    fun `GIVEN run reports ready WHEN collected THEN opens the feed in trading mode`() = runTest {
        // Arrange
        owesOnboarding()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.onStartClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                PolymarketRoute.Main(userWalletId = userWalletId),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the run failed WHEN start is pressed again THEN a new run is started`() = runTest {
        // Arrange
        owesOnboarding()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(
            PolymarketOnboardingProgress.Failed(
                error = PolymarketOnboardingError.Network,
                isRetryable = true,
            ),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.uiState.value.onStartClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onStartClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 2) { runOnboardingUseCase(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN start is tapped WHEN the run has not emitted yet THEN the button already spins`() = runTest {
        // Arrange
        owesOnboarding()
        every { runOnboardingUseCase(userWalletId) } returns emptyFlow()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.onStartClick()

        // Assert
        val state = model.uiState.value
        assertThat(state.isStarting).isTrue()
        advanceUntilIdle()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a run is in flight WHEN start is tapped again THEN the second tap is ignored`() = runTest {
        // Arrange
        owesOnboarding()
        every { runOnboardingUseCase(userWalletId) } returns flow {
            emit(PolymarketOnboardingProgress.Deriving)
            awaitCancellation()
        }
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val welcome = model.uiState.value
        welcome.onStartClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onStartClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { runOnboardingUseCase(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the welcome screen WHEN the Polymarket terms are tapped THEN the terms page is opened`() =
        runTest {
            // Arrange
            gateResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.uiState.value.onPolymarketTermsClick()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl("https://polymarket.com/tos") }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the welcome screen WHEN the Tangem terms are tapped THEN the terms page is opened`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.onTangemTermsClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/tangem_tos.html") }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the gate is open WHEN close is tapped THEN the gate is popped`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onCloseClick()

        // Assert
        verify(exactly = 1) { router.pop(onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the entry is still resolving WHEN the gate opens THEN the button already spins AND no overlay is shown`() =
        runTest {
            // Arrange
            coEvery { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) } coAnswers {
                delay(RESOLUTION_DELAY_MILLIS)
                PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
            }

            // Act
            val model = createModel(testScope = this)
            runCurrent()

            // Assert
            val state = model.uiState.value
            assertThat(state.isStarting).isTrue()
            assertThat(state.overlay).isNull()
            advanceUntilIdle()
            model.onDestroy()
        }

    internal data class ProgressModel(
        val progress: PolymarketOnboardingProgress,
        val expectedIsStarting: Boolean,
    )

    internal data class ResumeLabelModel(val status: PolymarketWalletStatus)

    /** Stubs what the gate sees when it opens — the resolution that is not allowed to prompt the user. */
    private fun gateResolves(entry: PolymarketEntry) {
        coEvery { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) } returns entry.right()
    }

    private fun gateFails(error: PolymarketOnboardingError) {
        coEvery { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) } returns error.left()
    }

    /** Stubs what pressing the action button sees — the full resolution, which may derive. */
    private fun startResolves(entry: PolymarketEntry) {
        coEvery { resolvePolymarketEntryInteractor(userWalletId) } returns entry.right()
    }

    private fun owesOnboarding() {
        val entry = PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED)
        gateResolves(entry)
        startResolves(entry)
    }

    private fun createModel(testScope: TestScope): PolymarketOnboardingModel = PolymarketOnboardingModel(
        paramsContainer = MutableParamsContainer(params),
        router = router,
        urlOpener = urlOpener,
        resolvePolymarketEntryInteractor = resolvePolymarketEntryInteractor,
        runPolymarketOnboardingInteractor = runOnboardingUseCase,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
    )

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        const val SUPERSEDED_ATTEMPT_DELAY_MILLIS = 1_000L
        const val RESOLUTION_DELAY_MILLIS = 1_000L
    }
}