package com.tangem.features.polymarket.impl.onboarding.model

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.res.R
import com.tangem.core.ui.coil.ImagePreloader
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.CheckNewPolymarketAccountAllowedInteractor
import com.tangem.domain.polymarket.interactor.ResolvePolymarketEntryInteractor
import com.tangem.domain.polymarket.interactor.RunPolymarketOnboardingInteractor
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketNewAccountAllowance
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.stories.GetStoryContentUseCase
import com.tangem.domain.stories.ShouldShowStoriesInteractor
import com.tangem.domain.stories.models.StoryContent
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

    private val checkNewAccountAllowed: CheckNewPolymarketAccountAllowedInteractor = mockk()
    private val resolvePolymarketEntryInteractor: ResolvePolymarketEntryInteractor = mockk()
    private val runOnboardingUseCase: RunPolymarketOnboardingInteractor = mockk()
    private val shouldShowStoriesInteractor: ShouldShowStoriesInteractor = mockk()
    private val getStoryContentUseCase: GetStoryContentUseCase = mockk()
    private val imagePreloader: ImagePreloader = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)

    private val userWalletId = UserWalletId("011")
    private val params = PolymarketOnboardingParams(userWalletId = userWalletId)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            checkNewAccountAllowed,
            resolvePolymarketEntryInteractor,
            runOnboardingUseCase,
            shouldShowStoriesInteractor,
            getStoryContentUseCase,
            imagePreloader,
            router,
            urlOpener,
            messageSender,
        )
        allowsNewAccount(PolymarketNewAccountAllowance.ALLOWED)
        storyWasSeen()
        coEvery { getStoryContentUseCase.invokeSync(id = any(), refresh = any()) } returns null.right()
    }

    @Test
    fun `GIVEN resolution fails WHEN model created THEN a snackbar is sent AND nothing is navigated`() =
        runTest {
            // Arrange
            gateFails(PolymarketOnboardingError.Network)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.welcome().isInProgress).isFalse()
            verify(exactly = 1) {
                messageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
            }
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
    fun `GIVEN entry is Onboard WHEN model created THEN the Welcome screen is idle AND nothing is navigated`() =
        runTest {
            // Arrange
            gateResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val state = model.welcome()
            assertThat(state.isInProgress).isFalse()
            assertThat(state.isRegionRestrictionsShown).isFalse()
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the gate failed to resolve WHEN start is tapped THEN the run decides`() = runTest {
        // Arrange
        gateFails(PolymarketOnboardingError.Network)
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { runOnboardingUseCase(userWalletId) }
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
                val state = model.welcome()
                assertThat(state.isInProgress).isFalse()
                assertThat(state.isRegionRestrictionsShown).isFalse()
                assertThat(state.actionButtonText)
                    .isEqualTo(resourceReference(R.string.prediction_onboarding_start_button))
                coVerify(exactly = 0) { resolvePolymarketEntryInteractor(userWalletId) }
                verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN an undetermined entry WHEN start is pressed THEN the run is launched`() = runTest {
            // Arrange
            gateResolves(PolymarketEntry.Undetermined)
            every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Deriving)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.welcome().onActionClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { runOnboardingUseCase(userWalletId) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN start is pressed WHEN the card tap is cancelled THEN the button returns to idle AND reports`() =
            runTest {
                // Arrange
                gateResolves(PolymarketEntry.Undetermined)
                every { runOnboardingUseCase(userWalletId) } returns flowOf(
                    PolymarketOnboardingProgress.Failed(
                        error = PolymarketOnboardingError.Derivation(PolymarketDerivationError.UserCancelled),
                        isRetryable = true,
                    ),
                )
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.welcome().onActionClick()
                advanceUntilIdle()

                // Assert
                val state = model.welcome()
                assertThat(state.isInProgress).isFalse()
                assertThat(state.isRegionRestrictionsShown).isFalse()
                verify(exactly = 1) {
                    messageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
                }
                verify(exactly = 1) { runOnboardingUseCase(userWalletId) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the region allows a new account WHEN start is tapped THEN the run starts without a second read`() =
            runTest {
                // Arrange
                gateResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))
                every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Deriving)
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.welcome().onActionClick()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 0) { resolvePolymarketEntryInteractor(userWalletId) }
                coVerify(exactly = 1) { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) }
                verify(exactly = 1) { runOnboardingUseCase(userWalletId) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the region refuses a new account WHEN start is pressed THEN nothing is asked of the user`() =
            runTest {
                // Arrange
                owesOnboarding()
                allowsNewAccount(PolymarketNewAccountAllowance.REGION_BLOCKED)
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.welcome().onActionClick()
                advanceUntilIdle()

                // Assert
                val state = model.welcome()
                assertThat(state.isRegionRestrictionsShown).isTrue()
                assertThat(state.isInProgress).isFalse()
                coVerify(exactly = 0) { resolvePolymarketEntryInteractor(userWalletId) }
                verify(exactly = 0) { runOnboardingUseCase(userWalletId) }
                verify(exactly = 0) { messageSender.send(any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the region cannot be read WHEN start is pressed THEN a snackbar is sent AND no sheet`() =
            runTest {
                // Arrange
                owesOnboarding()
                allowsNewAccount(PolymarketNewAccountAllowance.UNDETERMINED)
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.welcome().onActionClick()
                advanceUntilIdle()

                // Assert
                val state = model.welcome()
                assertThat(state.isRegionRestrictionsShown).isFalse()
                assertThat(state.isInProgress).isFalse()
                coVerify(exactly = 0) { resolvePolymarketEntryInteractor(userWalletId) }
                verify(exactly = 1) {
                    messageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the region sheet is shown WHEN it is dismissed THEN it closes AND nothing is navigated`() =
            runTest {
                // Arrange
                owesOnboarding()
                allowsNewAccount(PolymarketNewAccountAllowance.REGION_BLOCKED)
                val model = createModel(testScope = this)
                advanceUntilIdle()
                model.welcome().onActionClick()
                advanceUntilIdle()

                // Act
                model.welcome().onRegionRestrictionsDismiss()
                advanceUntilIdle()

                // Assert
                assertThat(model.welcome().isRegionRestrictionsShown).isFalse()
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
        model.welcome().onActionClick()
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
        assertThat(model.welcome().actionButtonText)
            .isEqualTo(resourceReference(R.string.prediction_onboarding_start_button))
        model.onDestroy()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ProgressMapping {

        private fun provideTestModels() = listOf(
            ProgressModel(progress = PolymarketOnboardingProgress.Deriving, expectedIsInProgress = true),
            ProgressModel(progress = PolymarketOnboardingProgress.AwaitingSignature, expectedIsInProgress = true),
            ProgressModel(
                progress = PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
                expectedIsInProgress = true,
            ),
            ProgressModel(
                progress = PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.Network,
                    isRetryable = true,
                ),
                expectedIsInProgress = false,
            ),
            ProgressModel(
                progress = PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.AddressMismatch(expected = "0xA", actual = "0xB"),
                    isRetryable = false,
                ),
                expectedIsInProgress = false,
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
            val idleState = subject.welcome()

            // Act
            idleState.onActionClick()
            advanceUntilIdle()

            // Assert
            assertThat(subject.uiState.value).isEqualTo(idleState.copy(isInProgress = model.expectedIsInProgress))
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
            assertThat(subject.welcome().actionButtonText)
                .isEqualTo(resourceReference(R.string.common_continue))
            subject.onDestroy()
        }
    }

    @Test
    fun `GIVEN the prefetch is in flight WHEN run reports ready THEN the story waits for it`() = runTest {
        // Arrange
        owesOnboarding()
        val release = storyArrivesOnRelease()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
        runCurrent()
        release.complete(Unit)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.push(AppRoute.Stories(storyId = STORY_ID, screenSource = STORY_SOURCE))
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the prefetch found nothing WHEN run reports ready THEN the content is fetched again`() = runTest {
        // Arrange
        owesOnboarding()
        storyIsUnseen()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { getStoryContentUseCase.invokeSync(id = STORY_ID, refresh = true) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is Onboarded WHEN model created THEN the story content is not prefetched`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Onboarded)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { getStoryContentUseCase.invokeSync(id = any(), refresh = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the story is unseen WHEN run reports ready THEN the story is shown over the feed`() = runTest {
        // Arrange
        owesOnboarding()
        storyIsUnseen()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.push(AppRoute.Stories(storyId = STORY_ID, screenSource = STORY_SOURCE))
        }
        verify(exactly = 1) { router.replaceAll(PolymarketRoute.Main(userWalletId = userWalletId)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the story was seen WHEN run reports ready THEN only the feed is opened`() = runTest {
        // Arrange
        owesOnboarding()
        storyWasSeen()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        verify(exactly = 1) { router.replaceAll(PolymarketRoute.Main(userWalletId = userWalletId)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is Onboarded WHEN model created THEN no story is shown`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Onboarded)
        storyIsUnseen()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the story content never arrives WHEN run reports ready THEN the feed opens without it`() = runTest {
        // Arrange
        owesOnboarding()
        storyIsUnseen()
        coEvery { getStoryContentUseCase.invokeSync(id = STORY_ID, refresh = true) } coAnswers { awaitCancellation() }
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        verify(exactly = 1) { router.replaceAll(PolymarketRoute.Main(userWalletId = userWalletId)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the story content arrives WHEN model created THEN its slide images are preloaded`() = runTest {
        // Arrange
        owesOnboarding()
        coEvery { getStoryContentUseCase.invokeSync(id = STORY_ID, refresh = true) } returns STORY_CONTENT.right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { imagePreloader.preload("https://host/first.webp") }
        verify(exactly = 1) { imagePreloader.preload("https://host/second.webp") }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the gate is opened WHEN model created THEN the story content is refreshed`() = runTest {
        // Arrange
        owesOnboarding()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getStoryContentUseCase.invokeSync(id = STORY_ID, refresh = true) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN run reports ready WHEN collected THEN opens the feed in trading mode`() = runTest {
        // Arrange
        owesOnboarding()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(PolymarketOnboardingProgress.Ready)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
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
        model.welcome().onActionClick()
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
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
        model.welcome().onActionClick()

        // Assert
        assertThat(model.welcome().isInProgress).isTrue()
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
        model.welcome().onActionClick()
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
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
            model.welcome().onPolymarketTermsClick()

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
        model.welcome().onTangemTermsClick()

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
    fun `GIVEN the entry is still resolving WHEN the gate opens THEN the Welcome screen is not shown yet`() =
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
            assertThat(model.uiState.value).isEqualTo(PolymarketOnboardingUM.Resolving)
            advanceUntilIdle()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a wallet already onboarded WHEN the gate opens THEN the Welcome screen never appears`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Onboarded)
        val model = createModel(testScope = this)

        // Act
        model.uiState.test {
            // Assert
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingUM.Resolving)
            advanceUntilIdle()
            expectNoEvents()
        }
        model.onDestroy()
    }


    @Test
    fun `GIVEN the run fails WHEN collected THEN the button returns to idle AND the failure is reported`() = runTest {
        // Arrange
        owesOnboarding()
        every { runOnboardingUseCase(userWalletId) } returns flowOf(
            PolymarketOnboardingProgress.Failed(error = PolymarketOnboardingError.Network, isRetryable = true),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.welcome().onActionClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.welcome().isInProgress).isFalse()
        verify(exactly = 1) {
            messageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
        }
        model.onDestroy()
    }

    /**
     * The use case reports cancellation as a failure, and the snackbar is global — a gate the user has left
     * would otherwise report itself on whatever screen they moved to.
     */
    @Test
    fun `GIVEN the gate is destroyed WHEN the in-flight resolution fails THEN nothing is reported`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) } coAnswers {
            runCatching { delay(RESOLUTION_DELAY_MILLIS) }
            PolymarketOnboardingError.Network.left()
        }
        val model = createModel(testScope = this)
        runCurrent()

        // Act
        model.onDestroy()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
    }

    @Test
    fun `GIVEN the gate is destroyed WHEN the in-flight run fails THEN nothing is reported`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Undetermined)
        every { runOnboardingUseCase(userWalletId) } returns flow {
            delay(RESOLUTION_DELAY_MILLIS)
            emit(
                PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.Derivation(PolymarketDerivationError.UserCancelled),
                    isRetryable = true,
                ),
            )
        }
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.welcome().onActionClick()
        runCurrent()

        // Act
        model.onDestroy()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
    }

    internal data class ProgressModel(
        val progress: PolymarketOnboardingProgress,
        val expectedIsInProgress: Boolean,
    )

    internal data class ResumeLabelModel(val status: PolymarketWalletStatus)

    @Test
    fun `GIVEN entry is Reconnect WHEN model created THEN the Reconnect screen is idle AND nothing is navigated`() =
        runTest {
            // Arrange
            gateResolves(PolymarketEntry.Reconnect)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.reconnect().isInProgress).isFalse()
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the Reconnect screen WHEN the action is tapped THEN the same onboarding run is started`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Reconnect)
        every { runOnboardingUseCase(userWalletId) } returns emptyFlow()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.reconnect().onActionClick()

        // Assert
        assertThat(model.reconnect().isInProgress).isTrue()
        advanceUntilIdle()
        verify(exactly = 1) { runOnboardingUseCase(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the Reconnect screen WHEN the run fails THEN the button goes idle again`() = runTest {
        // Arrange
        gateResolves(PolymarketEntry.Reconnect)
        every { runOnboardingUseCase(userWalletId) } returns flowOf(
            PolymarketOnboardingProgress.Failed(error = PolymarketOnboardingError.Network, isRetryable = true),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.reconnect().onActionClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.reconnect().isInProgress).isFalse()
        model.onDestroy()
    }

    private fun PolymarketOnboardingModel.welcome(): PolymarketOnboardingUM.Welcome =
        uiState.value as PolymarketOnboardingUM.Welcome

    private fun PolymarketOnboardingModel.reconnect(): PolymarketOnboardingUM.Reconnect =
        uiState.value as PolymarketOnboardingUM.Reconnect

    /** Stubs what the gate sees when it opens — the resolution that is not allowed to prompt the user. */
    private fun gateResolves(entry: PolymarketEntry) {
        coEvery { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) } returns entry.right()
    }

    private fun gateFails(error: PolymarketOnboardingError) {
        coEvery { resolvePolymarketEntryInteractor.withoutPrompting(userWalletId) } returns error.left()
    }

    private fun storyIsUnseen() {
        coEvery { shouldShowStoriesInteractor.invokeSync(STORY_ID) } returns true
    }

    private fun storyArrivesOnRelease(): CompletableDeferred<Unit> {
        val release = CompletableDeferred<Unit>()
        var delivered = false
        coEvery { getStoryContentUseCase.invokeSync(id = STORY_ID, refresh = true) } coAnswers {
            release.await()
            delivered = true
            STORY_CONTENT.right()
        }
        coEvery { shouldShowStoriesInteractor.invokeSync(STORY_ID) } answers { delivered }
        return release
    }

    private fun storyWasSeen() {
        coEvery { shouldShowStoriesInteractor.invokeSync(STORY_ID) } returns false
    }

    private fun allowsNewAccount(allowance: PolymarketNewAccountAllowance) {
        coEvery { checkNewAccountAllowed(userWalletId) } returns allowance
    }

    private fun owesOnboarding() {
        gateResolves(PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED))
    }

    private fun createModel(testScope: TestScope): PolymarketOnboardingModel = PolymarketOnboardingModel(
        paramsContainer = MutableParamsContainer(params),
        router = router,
        urlOpener = urlOpener,
        messageSender = messageSender,
        checkNewPolymarketAccountAllowed = checkNewAccountAllowed,
        resolvePolymarketEntryInteractor = resolvePolymarketEntryInteractor,
        runPolymarketOnboardingInteractor = runOnboardingUseCase,
        shouldShowStoriesInteractor = shouldShowStoriesInteractor,
        getStoryContentUseCase = getStoryContentUseCase,
        imagePreloader = imagePreloader,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
    )

    private companion object {
        const val RESOLUTION_DELAY_MILLIS = 1_000L
        val STORY_ID = StoryContentIds.STORY_POLYMARKET_ONBOARDING.id
        const val STORY_SOURCE = "Onboarding"
        val STORY_CONTENT = StoryContent(
            imageHost = "https://host/",
            story = StoryContent.Story(
                id = STORY_ID,
                title = null,
                slides = listOf(StoryContent.StorySlide(id = "first"), StoryContent.StorySlide(id = "second")),
            ),
        )
    }
}