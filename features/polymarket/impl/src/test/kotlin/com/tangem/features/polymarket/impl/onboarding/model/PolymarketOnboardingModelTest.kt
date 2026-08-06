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
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.ResolvePolymarketEntryUseCase
import com.tangem.domain.polymarket.usecase.RunPolymarketOnboardingUseCase
import com.tangem.features.polymarket.api.PolymarketComponent
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

    private val resolvePolymarketEntryUseCase: ResolvePolymarketEntryUseCase = mockk()
    private val runOnboardingUseCase: RunPolymarketOnboardingUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)

    private val userWalletId = UserWalletId("011")
    private val params = PolymarketComponent.Params(userWalletId = userWalletId)

    @BeforeEach
    fun resetMocks() {
        clearMocks(resolvePolymarketEntryUseCase, runOnboardingUseCase, router, urlOpener)
    }

    @Test
    fun `GIVEN resolution fails WHEN model created THEN the error overlay is raised AND nothing is navigated`() =
        runTest {
            // Arrange
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns PolymarketOnboardingError.Network.left()

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
    fun `GIVEN entry is Trade WHEN model created THEN the feed is opened in trading mode`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.TRADING).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.TRADING)),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is ReadOnly WHEN model created THEN the feed is opened in read-only mode`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.READ_ONLY).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.READ_ONLY)),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is Onboard WHEN model created THEN no overlay is shown AND nothing is navigated`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()

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
    fun `GIVEN entry is RegionBlocked WHEN model created THEN the sheet overlay is raised AND nothing is navigated`() =
        runTest {
            // Arrange
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns PolymarketEntry.RegionBlocked.right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state.overlay).isInstanceOf(PolymarketOnboardingUM.Overlay.RegionRestrictions::class.java)
            assertThat(state.isStarting).isFalse()
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the sheet overlay is raised WHEN it is dismissed THEN the feed is opened in read-only mode`() =
        runTest {
            // Arrange
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns PolymarketEntry.RegionBlocked.right()
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val overlay = model.uiState.value.overlay as PolymarketOnboardingUM.Overlay.RegionRestrictions

            // Act
            overlay.onDismiss()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.READ_ONLY)),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the error overlay is raised WHEN retry is tapped THEN the entry is resolved again`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returnsMany listOf(
            PolymarketOnboardingError.Network.left(),
            PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.TRADING).right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value.overlay as PolymarketOnboardingUM.Overlay.Error).onRetryClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { resolvePolymarketEntryUseCase(userWalletId) }
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.TRADING)),
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
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } coAnswers {
                when (++attempt) {
                    1 -> PolymarketOnboardingError.Network.left()
                    2 -> {
                        runCatching { delay(SUPERSEDED_ATTEMPT_DELAY_MILLIS) }
                        PolymarketOnboardingError.Network.left()
                    }
                    else -> PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.TRADING).right()
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
                    routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.TRADING)),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN entry owes onboarding WHEN start clicked THEN run is launched`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
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
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()

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
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
                PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
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
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
                PolymarketEntry.Onboard(status = model.status).right()

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
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.onStartClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(PolymarketRoute.Main(accessMode = PolymarketAccessMode.TRADING))
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the run failed WHEN start is pressed again THEN a new run is started`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
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
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
        every { runOnboardingUseCase(userWalletId) } returns emptyFlow()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.onStartClick()

        // Assert
        val state = model.uiState.value
        assertThat(state.isStarting).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a run is in flight WHEN start is tapped again THEN the second tap is ignored`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
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
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
                PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
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
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
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
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()
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
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } coAnswers {
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

    private fun createModel(testScope: TestScope): PolymarketOnboardingModel = PolymarketOnboardingModel(
        paramsContainer = MutableParamsContainer(params),
        router = router,
        urlOpener = urlOpener,
        resolvePolymarketEntryUseCase = resolvePolymarketEntryUseCase,
        runPolymarketOnboardingUseCase = runOnboardingUseCase,
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