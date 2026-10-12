package com.tangem.tap.domain.scanCard

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.card.ScanFailsCounter
import com.tangem.domain.card.common.util.twinsIsTwinned
import com.tangem.domain.card.models.ScanConfig
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultScanCardProcessorTest {

    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxed = true)
    private val scanFailsCounter: ScanFailsCounter = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk()
    private val wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase = mockk()
    private val onboardingHelper: OnboardingHelper = mockk()
    private val tangemSdkManager: TangemSdkManager = mockk(relaxed = true)

    private val scanResponse: ScanResponse = mockk()
    private val source = AnalyticsParam.ScreensSources.Intro

    @BeforeAll
    fun mockScanResponseExtensions() {
        mockkStatic(SCAN_RESPONSE_EXT)
    }

    @AfterAll
    fun unmockScanResponseExtensions() {
        unmockkStatic(SCAN_RESPONSE_EXT)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            uiMessageSender,
            analyticsEventHandler,
            trackingContextProxy,
            scanFailsCounter,
            appRouter,
            sendFeedbackEmailUseCase,
            wasTwinsOnboardingShownUseCase,
            onboardingHelper,
            tangemSdkManager,
        )
        every { appRouter.push(any(), any()) } just runs
        coEvery { sendFeedbackEmailUseCase(any()) } just runs
        coEvery { onboardingHelper.isOnboardingCase(any()) } returns false
        coEvery { wasTwinsOnboardingShownUseCase.invokeSync() } returns true
        every { scanResponse.twinsIsTwinned() } returns false
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScanWithCallbacks {

        @Test
        fun `GIVEN card with wallet WHEN scan THEN success delivered and fails counter reset`() = runTest {
            // Arrange
            val processor = createProcessor()
            val callbacks = RecordingCallbacks()
            stubScanResult(CompletionResult.Success(scanResponse))

            // Act
            callbacks.scanWith(processor)
            advanceUntilIdle()

            // Assert
            assertThat(callbacks.events).containsExactly("progress:true", "success").inOrder()
            verify(exactly = 1) { scanFailsCounter.reset() }
            verify(exactly = 1) { trackingContextProxy.setContext(scanResponse) }
            verify(exactly = 1) { tangemSdkManager.changeDisplayedCardIdNumbersCount(null) }
            verify(exactly = 1) { tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse) }
            verify(exactly = 0) { appRouter.push(any(), any()) }
        }

        @Test
        fun `GIVEN card without wallet WHEN scan THEN navigates to onboarding and hides progress`() = runTest {
            // Arrange
            val processor = createProcessor()
            val callbacks = RecordingCallbacks()
            stubScanResult(CompletionResult.Success(scanResponse))
            coEvery { onboardingHelper.isOnboardingCase(scanResponse) } returns true

            // Act
            callbacks.scanWith(processor)
            advanceUntilIdle()

            // Assert
            assertThat(callbacks.events)
                .containsExactly("progress:true", "walletNotCreated", "progress:false")
                .inOrder()
            verify(exactly = 1) { trackingContextProxy.addContext(scanResponse) }
            verify(exactly = 1) {
                appRouter.push(
                    AppRoute.Onboarding(scanResponse = scanResponse, mode = AppRoute.Onboarding.Mode.Onboarding),
                    any(),
                )
            }
        }

        @Test
        fun `GIVEN twinned card without twins welcome WHEN scan THEN navigates to twins welcome`() = runTest {
            // Arrange
            val processor = createProcessor()
            val callbacks = RecordingCallbacks()
            stubScanResult(CompletionResult.Success(scanResponse))
            every { scanResponse.twinsIsTwinned() } returns true
            coEvery { wasTwinsOnboardingShownUseCase.invokeSync() } returns false

            // Act
            callbacks.scanWith(processor)
            advanceUntilIdle()

            // Assert
            assertThat(callbacks.events)
                .containsExactly("progress:true", "walletNotCreated", "progress:false")
                .inOrder()
            verify(exactly = 1) {
                appRouter.push(
                    AppRoute.Onboarding(scanResponse = scanResponse, mode = AppRoute.Onboarding.Mode.WelcomeOnlyTwin),
                    any(),
                )
            }
        }

        @Test
        fun `GIVEN user cancelled scan WHEN scan THEN failure is delivered and counted as cancellation`() = runTest {
            // Arrange
            val processor = createProcessor()
            val callbacks = RecordingCallbacks()
            val error = TangemSdkError.UserCancelled()
            stubScanResult(CompletionResult.Failure(error))

            // Act
            callbacks.scanWith(processor)
            advanceUntilIdle()

            // Assert
            assertThat(callbacks.events).containsExactly("progress:true", "failure").inOrder()
            assertThat(callbacks.failure).isSameInstanceAs(error)
            verify(exactly = 1) { scanFailsCounter.onScanFailure(isUserCancelled = true, source = source) }
        }

        @Test
        fun `GIVEN sdk error WHEN scan THEN original error is delivered and not counted as cancellation`() = runTest {
            // Arrange
            val processor = createProcessor()
            val callbacks = RecordingCallbacks()
            val error = TangemSdkError.NfcFeatureIsUnavailable()
            stubScanResult(CompletionResult.Failure(error))

            // Act
            callbacks.scanWith(processor)
            advanceUntilIdle()

            // Assert
            assertThat(callbacks.failure).isSameInstanceAs(error)
            verify(exactly = 1) { scanFailsCounter.onScanFailure(isUserCancelled = false, source = source) }
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN card verification failed WHEN scan THEN attestation dialog is shown instead of failure`() = runTest {
            // Arrange
            val processor = createProcessor()
            val callbacks = RecordingCallbacks()
            stubScanResult(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))

            // Act
            callbacks.scanWith(processor)
            advanceUntilIdle()

            // Assert
            assertThat(callbacks.events).containsExactly("progress:true")
            verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(OnboardingAnalyticsEvent.Error.OfflineAttestationFailed(source))
            }
        }

        @Test
        fun `GIVEN attestation dialog WHEN cancel clicked THEN progress is hidden and cancel is delivered`() = runTest {
            // Arrange
            val processor = createProcessor()
            val callbacks = RecordingCallbacks()
            val dialog = captureDialog()
            stubScanResult(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))
            callbacks.scanWith(processor)
            advanceUntilIdle()

            // Act
            requireNotNull(dialog.captured.secondAction).onClick()
            advanceUntilIdle()

            // Assert
            assertThat(callbacks.events).containsExactly("progress:true", "progress:false", "cancel").inOrder()
            coVerify(exactly = 0) { sendFeedbackEmailUseCase(any()) }
        }

        @Test
        fun `GIVEN attestation dialog WHEN support requested THEN cancel delivered and email opened`() = runTest {
            // Arrange
            val processor = createProcessor()
            val callbacks = RecordingCallbacks()
            val dialog = captureDialog()
            stubScanResult(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))
            callbacks.scanWith(processor)
            advanceUntilIdle()

            // Act
            dialog.captured.firstAction.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(callbacks.events).containsExactly("progress:true", "progress:false", "cancel").inOrder()
            coVerify(exactly = 1) { sendFeedbackEmailUseCase(FeedbackEmailType.CardAttestationFailed) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScanReturningResult {

        @Test
        fun `GIVEN successful scan WHEN scan THEN returns sdk result without post processing`() = runTest {
            // Arrange
            val processor = createProcessor()
            val expected = CompletionResult.Success(scanResponse)
            stubScanResult(expected)

            // Act
            val actual = processor.scan(
                cardId = CARD_ID,
                allowsRequestAccessCodeFromRepository = true,
                analyticsSource = source,
                shouldCheckIsAlreadyActivated = false,
            )

            // Assert
            assertThat(actual).isEqualTo(expected)
            coVerify(exactly = 1) {
                tangemSdkManager.scanProduct(
                    cardId = CARD_ID,
                    messageRes = any(),
                    allowsRequestAccessCodeFromRepository = true,
                    shouldCheckIsAlreadyActivated = false,
                    source = source,
                )
            }
            verify(exactly = 0) { scanFailsCounter.reset() }
            verify(exactly = 0) { appRouter.push(any(), any()) }
        }

        @Test
        fun `GIVEN card verification failed WHEN scan THEN shows attestation dialog and returns failure`() = runTest {
            // Arrange
            val processor = createProcessor()
            val expected = CompletionResult.Failure<ScanResponse>(TangemSdkError.CardVerificationFailed())
            stubScanResult(expected)

            // Act
            val actual = processor.scan(
                cardId = null,
                allowsRequestAccessCodeFromRepository = false,
                analyticsSource = source,
                shouldCheckIsAlreadyActivated = true,
            )

            // Assert
            assertThat(actual).isEqualTo(expected)
            verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScanWithSignInConfig {

        private val callback: ScanConfig.Callback = mockk(relaxed = true)
        private val config = ScanConfig.signIn(source, callback)

        @BeforeEach
        fun resetCallback() {
            clearMocks(callback)
        }

        @Test
        fun `GIVEN card with wallet WHEN scan THEN success delivered and fails counter reset`() = runTest {
            // Arrange
            val processor = createProcessor()
            stubScanResult(CompletionResult.Success(scanResponse))

            // Act
            processor.scan(config)
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                callback.onProgressStateChange(showProgress = true)
                callback.onSuccess(scanResponse)
            }
            coVerify(exactly = 1) {
                tangemSdkManager.scanProduct(
                    cardId = null,
                    messageRes = any(),
                    allowsRequestAccessCodeFromRepository = false,
                    shouldCheckIsAlreadyActivated = true,
                    source = source,
                )
            }
            verify(exactly = 1) { scanFailsCounter.reset() }
            verify(exactly = 1) { trackingContextProxy.setContext(scanResponse) }
            verify(exactly = 1) { tangemSdkManager.changeDisplayedCardIdNumbersCount(null) }
            verify(exactly = 1) { tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse) }
        }

        @Test
        fun `GIVEN card id in config WHEN scan THEN scans only that card`() = runTest {
            // Arrange
            val processor = createProcessor()
            stubScanResult(CompletionResult.Success(scanResponse))

            // Act
            processor.scan(config.copy(cardId = CARD_ID))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                tangemSdkManager.scanProduct(
                    cardId = CARD_ID,
                    messageRes = any(),
                    allowsRequestAccessCodeFromRepository = any(),
                    shouldCheckIsAlreadyActivated = any(),
                    source = source,
                )
            }
        }

        @Test
        fun `GIVEN card without wallet WHEN scan THEN onboarding required and launched`() = runTest {
            // Arrange
            val processor = createProcessor()
            stubScanResult(CompletionResult.Success(scanResponse))
            coEvery { onboardingHelper.isOnboardingCase(scanResponse) } returns true

            // Act
            processor.scan(config)
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                callback.onProgressStateChange(showProgress = true)
                callback.onOnboardingRequired()
                callback.onProgressStateChange(showProgress = false)
            }
            coVerify(exactly = 0) { callback.onSuccess(any()) }
            verify(exactly = 1) {
                appRouter.push(
                    AppRoute.Onboarding(scanResponse = scanResponse, mode = AppRoute.Onboarding.Mode.Onboarding),
                    any(),
                )
            }
        }

        @Test
        fun `GIVEN user cancelled scan WHEN scan THEN failure delivered and counted as cancellation`() = runTest {
            // Arrange
            val processor = createProcessor()
            val error = TangemSdkError.UserCancelled()
            stubScanResult(CompletionResult.Failure(error))

            // Act
            processor.scan(config)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { callback.onFailure(error) }
            verify(exactly = 1) { scanFailsCounter.onScanFailure(isUserCancelled = true, source = source) }
        }

        @Test
        fun `GIVEN attestation dialog WHEN cancel clicked THEN progress hidden and failure reported`() = runTest {
            // Arrange
            val processor = createProcessor()
            val dialog = captureDialog()
            stubScanResult(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))
            processor.scan(config)
            advanceUntilIdle()

            // Act
            requireNotNull(dialog.captured.secondAction).onClick()
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                callback.onProgressStateChange(showProgress = true)
                callback.onProgressStateChange(showProgress = false)
                callback.onCardVerificationFailed()
            }
            coVerify(exactly = 0) { callback.onFailure(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScanWithRescanKnownCardConfig {

        private val callback: ScanConfig.Callback = mockk(relaxed = true)
        private val config = ScanConfig.rescanKnownCard(source, callback)

        @BeforeEach
        fun resetCallback() {
            clearMocks(callback)
        }

        @Test
        fun `GIVEN card without wallet WHEN scan THEN success delivered without post processing`() = runTest {
            // Arrange
            val processor = createProcessor()
            stubScanResult(CompletionResult.Success(scanResponse))
            coEvery { onboardingHelper.isOnboardingCase(scanResponse) } returns true

            // Act
            processor.scan(config)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { callback.onSuccess(scanResponse) }
            coVerify(exactly = 1) {
                tangemSdkManager.scanProduct(
                    cardId = null,
                    messageRes = any(),
                    allowsRequestAccessCodeFromRepository = true,
                    shouldCheckIsAlreadyActivated = false,
                    source = source,
                )
            }
            coVerify(exactly = 0) { onboardingHelper.isOnboardingCase(any()) }
            verify(exactly = 0) { appRouter.push(any(), any()) }
            verify(exactly = 0) { scanFailsCounter.reset() }
            verify(exactly = 0) { tangemSdkManager.changeDisplayedCardIdNumbersCount(any()) }
        }

        @Test
        fun `GIVEN user cancelled scan WHEN scan THEN failure delivered without counting it`() = runTest {
            // Arrange
            val processor = createProcessor()
            val error = TangemSdkError.UserCancelled()
            stubScanResult(CompletionResult.Failure(error))

            // Act
            processor.scan(config)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { callback.onFailure(error) }
            verify(exactly = 0) { scanFailsCounter.onScanFailure(any(), any()) }
        }
    }

    private fun TestScope.createProcessor(): DefaultScanCardProcessor {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestingCoroutineDispatcherProvider(
            main = dispatcher,
            mainImmediate = dispatcher,
            io = dispatcher,
            default = dispatcher,
            single = dispatcher,
        )

        return DefaultScanCardProcessor(
            uiMessageSender = uiMessageSender,
            analyticsEventHandler = analyticsEventHandler,
            trackingContextProxy = trackingContextProxy,
            scanFailsCounter = scanFailsCounter,
            appRouter = appRouter,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            wasTwinsOnboardingShownUseCase = wasTwinsOnboardingShownUseCase,
            onboardingHelper = onboardingHelper,
            tangemSdkManager = tangemSdkManager,
            appCoroutineScope = TestAppCoroutineScope(this),
            dispatchers = dispatchers,
        )
    }

    private fun stubScanResult(result: CompletionResult<ScanResponse>) {
        coEvery {
            tangemSdkManager.scanProduct(
                cardId = any(),
                messageRes = any(),
                allowsRequestAccessCodeFromRepository = any(),
                shouldCheckIsAlreadyActivated = any(),
                source = any(),
            )
        } returns result
    }

    private fun captureDialog(): CapturingSlot<DialogMessage> {
        val dialog = slot<DialogMessage>()
        every { uiMessageSender.send(capture(dialog)) } just runs
        return dialog
    }

    private inner class RecordingCallbacks {

        val events = mutableListOf<String>()
        var failure: TangemError? = null
            private set

        suspend fun scanWith(processor: DefaultScanCardProcessor) {
            processor.scan(
                analyticsSource = source,
                shouldCheckIsAlreadyActivated = true,
                cardId = null,
                onProgressStateChange = { events += "progress:$it" },
                onWalletNotCreated = { events += "walletNotCreated" },
                onCancel = { events += "cancel" },
                onFailure = { error ->
                    events += "failure"
                    failure = error
                },
                onSuccess = { events += "success" },
            )
        }
    }

    private companion object {
        const val CARD_ID = "AC01000000000001"
        const val SCAN_RESPONSE_EXT = "com.tangem.domain.card.common.util.ScanResponseExtKt"
    }
}