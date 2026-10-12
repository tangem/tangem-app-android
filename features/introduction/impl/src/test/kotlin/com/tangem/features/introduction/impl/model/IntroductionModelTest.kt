package com.tangem.features.introduction.impl.model

import android.view.SurfaceView
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletCreationNotSupportedDialog
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.analytics.repository.AnalyticsRepository
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.card.analytics.IntroductionProcess.LegalOpened
import com.tangem.domain.card.analytics.IntroductionProcess.SheetDismissed
import com.tangem.domain.card.analytics.IntroductionProcess.WelcomeMediaError
import com.tangem.domain.card.analytics.Shop
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.restore.CloudRestoreResult
import com.tangem.domain.cloudbackup.usecase.ResolveCloudRestoreBackupsUseCase
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.cloudrestore.CloudRestoreErrorMessage
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.features.introduction.impl.ui.state.CreateWalletUM
import com.tangem.features.storiesv2.StoriesV2PendingAction
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.features.storiesv2.StoryV2ActionTarget
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import arrow.core.left
import arrow.core.right
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import com.tangem.core.res.R as CoreResR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionModelTest {

    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val videoPlayer: IntroductionVideoPlayer = mockk(relaxed = true)
    private val videoPlayerFactory: IntroductionVideoPlayer.Factory = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk()
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles = mockk()
    private val hotWalletFeatureToggles: HotWalletFeatureToggles = mockk()
    private val resolveCloudRestoreBackups: ResolveCloudRestoreBackupsUseCase = mockk()
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxed = true)
    private val cardScanner: IntroductionCardScanner = mockk(relaxed = true)
    private val mobileWalletCreator: IntroductionMobileWalletCreator = mockk(relaxed = true)
    private val appsFlyerRepository: AppsFlyerRepository = mockk()
    private val analyticsRepository: AnalyticsRepository = mockk(relaxed = true)
    private var entryReasonTracker = IntroductionEntryReasonTracker(analyticsRepository, analyticsEventHandler)
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val surfaceView: SurfaceView = mockk(relaxed = true)
    private val otherSurfaceView: SurfaceView = mockk(relaxed = true)

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            urlOpener,
            analyticsEventHandler,
            videoPlayer,
            videoPlayerFactory,
            router,
            isHotWalletCreationSupported,
            socialLoginFeatureToggles,
            hotWalletFeatureToggles,
            resolveCloudRestoreBackups,
            trackingContextProxy,
            cardScanner,
            mobileWalletCreator,
            appsFlyerRepository,
            analyticsRepository,
            generateBuyTangemCardLinkUseCase,
            uiMessageSender,
        )
        coEvery { appsFlyerRepository.getConversionData() } returns null
        coEvery { analyticsRepository.checkIsEventSent(any()) } returns true
        entryReasonTracker = IntroductionEntryReasonTracker(analyticsRepository, analyticsEventHandler)
        every { isHotWalletCreationSupported() } returns true
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns true
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        sentEvents.clear()
        every { videoPlayer.isMotionEnabled } returns true
        surfaceAttached(isAttached = false)
        every { videoPlayerFactory.create(any(), any()) } returns videoPlayer
        every { analyticsEventHandler.send(capture(sentEvents)) } just runs
    }

    @Test
    fun `GIVEN nothing rendered yet WHEN model created THEN shutter is down and the screen is opened once`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.isVideoReady).isFalse()
        assertThat(model.uiState.value.isMotionEnabled).isTrue()
        assertThat(sentEvents.map { it::class }).containsExactly(IntroductionProcess.ScreenOpened::class)
    }

    @Test
    fun `GIVEN surface returns after leaving the screen WHEN each frame arrives THEN shutter follows the surface`() {
        // Arrange
        val model = createModel()

        // Act & Assert
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.renderFrame()
        assertThat(model.uiState.value.isVideoReady).isTrue()

        surfaceAttached(isAttached = false)
        model.detachSurface(surfaceView)
        assertThat(model.uiState.value.isVideoReady).isFalse()

        surfaceAttached(isAttached = true)
        model.attachSurface(otherSurfaceView)
        assertThat(model.uiState.value.isVideoReady).isFalse()

        model.renderFrame()
        assertThat(model.uiState.value.isVideoReady).isTrue()
    }

    @Test
    fun `GIVEN nothing decoded yet WHEN model created THEN the surface already has the size of the video`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.videoSize).isEqualTo(VIDEO_SIZE)
        assertThat(model.uiState.value.videoSurfaceKey).isEqualTo(0)
    }

    @Test
    fun `GIVEN the declared size WHEN the decoder confirms it THEN the surface keeps its size`() {
        // Arrange
        val model = createModel()

        // Act
        model.onVideoSizeChanged(width = VIDEO_SIZE.width, height = VIDEO_SIZE.height)

        // Assert
        assertThat(model.uiState.value.videoSize).isEqualTo(VIDEO_SIZE)
    }

    @Test
    fun `GIVEN a file of another size WHEN the decoder reports it THEN the surface keeps the declared size`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.renderFrame()

        // Act
        model.onVideoSizeChanged(width = 720, height = 1280)

        // Assert
        assertThat(model.uiState.value.videoSize).isEqualTo(VIDEO_SIZE)
        assertThat(model.uiState.value.videoSurfaceKey).isEqualTo(0)
        assertThat(model.uiState.value.isVideoReady).isTrue()
    }

    @Test
    fun `GIVEN a replacement surface WHEN the previous one is released THEN the picture stays uncovered`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.renderFrame()
        model.attachSurface(otherSurfaceView)

        // Act
        model.detachSurface(surfaceView)

        // Assert
        assertThat(model.uiState.value.isVideoReady).isTrue()
        model.onError(PLAYBACK_ERROR)
        assertThat(model.uiState.value.isVideoReady).isTrue()
    }

    @Test
    fun `GIVEN nothing rendered yet WHEN playback fails THEN shutter stays down over the empty surface`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)

        // Act
        model.onError(PLAYBACK_ERROR)

        // Assert
        assertThat(model.uiState.value.isVideoReady).isFalse()
        assertThat(model.uiState.value.isVideoFailed).isTrue()
        assertThat(sentEvents.last()).isEqualTo(WelcomeMediaError(WelcomeMediaError.Stage.Load))
    }

    @Test
    fun `GIVEN a frame already rendered WHEN playback fails THEN shutter stays up on the still frame`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.renderFrame()

        // Act
        model.onError(PLAYBACK_ERROR)

        // Assert
        assertThat(model.uiState.value.isVideoReady).isTrue()
        assertThat(model.uiState.value.isVideoFailed).isTrue()
        assertThat(sentEvents.last()).isEqualTo(WelcomeMediaError(WelcomeMediaError.Stage.Playback))
    }

    @Test
    fun `GIVEN a fresh surface after a rendered frame WHEN playback fails THEN shutter stays down`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.renderFrame()
        surfaceAttached(isAttached = false)
        model.detachSurface(surfaceView)
        surfaceAttached(isAttached = true)
        model.attachSurface(otherSurfaceView)

        // Act
        model.onError(PLAYBACK_ERROR)

        // Assert
        assertThat(model.uiState.value.isVideoReady).isFalse()
        assertThat(model.uiState.value.isVideoFailed).isTrue()
    }

    @Test
    fun `GIVEN surface already gone WHEN playback fails THEN shutter stays down`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.renderFrame()
        surfaceAttached(isAttached = false)
        model.detachSurface(surfaceView)

        // Act
        model.onError(PLAYBACK_ERROR)

        // Assert
        assertThat(model.uiState.value.isVideoReady).isFalse()
        assertThat(model.uiState.value.isVideoFailed).isTrue()
        assertThat(sentEvents.last()).isEqualTo(WelcomeMediaError(WelcomeMediaError.Stage.Playback))
    }

    @Test
    fun `GIVEN reduced motion WHEN model created THEN state reports motion disabled`() {
        // Arrange
        every { videoPlayer.isMotionEnabled } returns false

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.isMotionEnabled).isFalse()
    }

    @Test
    fun `GIVEN playback under way WHEN the video position is read THEN the player position is returned`() {
        // Arrange
        every { videoPlayer.positionMs } returns 4_200L
        val model = createModel()

        // Act
        val positionMs = model.videoPositionMs()

        // Assert
        assertThat(positionMs).isEqualTo(4_200L)
    }

    @Test
    fun `GIVEN introduction screen WHEN destroyed twice THEN player released once per call`() {
        // Arrange
        val model = createModel()

        // Act
        model.onDestroy()
        model.onDestroy()

        // Assert
        verify(exactly = 2) { videoPlayer.release() }
    }

    @Test
    fun `GIVEN introduction screen WHEN terms of service clicked THEN opens terms of service`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onTermsOfServiceClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/tangem_tos.html") }
        assertThat(sentEvents).containsExactly(
            SCREEN_OPENED,
            LegalOpened(LegalOpened.Document.TermsOfService),
        ).inOrder()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EntryReason {

        @Test
        fun `GIVEN the welcome was never shown WHEN model created THEN the opening reports the first launch`() {
            // Arrange
            coEvery { analyticsRepository.checkIsEventSent("introduction_welcome_opened") } returns false

            // Act
            createModel()

            // Assert
            assertThat(sentEvents).containsExactly(
                IntroductionProcess.ScreenOpened(IntroductionProcess.ScreenOpened.EntryReason.FirstLaunch),
            )
            coVerify(exactly = 1) { analyticsRepository.setIsEventSent("introduction_welcome_opened") }
        }

        @Test
        fun `GIVEN the welcome was shown before WHEN model created THEN the opening reports no wallets`() {
            // Act
            createModel()

            // Assert
            assertThat(sentEvents).containsExactly(SCREEN_OPENED)
            coVerify(exactly = 0) { analyticsRepository.setIsEventSent(any()) }
        }

        @Test
        fun `GIVEN the opening was reported WHEN the screen is rebuilt in the same process THEN it is not reported again`() {
            // Arrange
            createModel()

            // Act
            createModel()

            // Assert
            assertThat(sentEvents).containsExactly(SCREEN_OPENED)
        }

        @Test
        fun `GIVEN the opening was reported WHEN the last wallet is forgotten THEN the new opening is reported`() {
            // Arrange
            createModel()

            // Act
            createModel(entryReason = AppRoute.Home.EntryReason.LastWalletForgotten)

            // Assert
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ScreenOpened(IntroductionProcess.ScreenOpened.EntryReason.LastWalletForgotten),
            ).inOrder()
        }

        @Test
        fun `GIVEN the last wallet was forgotten WHEN model created THEN the opening reports it regardless of history`() {
            // Arrange
            coEvery { analyticsRepository.checkIsEventSent(any()) } returns false

            // Act
            createModel(entryReason = AppRoute.Home.EntryReason.LastWalletForgotten)

            // Assert
            assertThat(sentEvents).containsExactly(
                IntroductionProcess.ScreenOpened(IntroductionProcess.ScreenOpened.EntryReason.LastWalletForgotten),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LaunchMode {

        @Test
        fun `GIVEN an NFC launch WHEN model created THEN the scan starts without a tap being reported`() {
            // Act
            val model = createModel(launchMode = InitScreenLaunchMode.WithCardScan)

            // Assert
            coVerify(exactly = 1) {
                cardScanner.scanAndOpenWallet(
                    analyticsSource = AnalyticsParam.ScreensSources.Intro,
                    onNavigatedAway = any(),
                    onFailed = any(),
                )
            }
            assertThat(sentEvents).containsExactly(SCREEN_OPENED)
            assertThat(model.uiState.value.renderedFields()).isEqualTo(SHUTTER_DOWN)
        }

        @Test
        fun `GIVEN a standard launch WHEN model created THEN no scan starts`() {
            // Act
            createModel(launchMode = InitScreenLaunchMode.Standard)

            // Assert
            coVerify(exactly = 0) { cardScanner.scanAndOpenWallet(any(), any(), any()) }
            assertThat(sentEvents).containsExactly(SCREEN_OPENED)
        }

        @Test
        fun `GIVEN an NFC launch WHEN the video is stopped and started again THEN the scan does not restart`() {
            // Arrange
            val model = createModel(launchMode = InitScreenLaunchMode.WithCardScan)

            // Act
            model.setVideoRunning(false)
            model.setVideoRunning(true)

            // Assert
            coVerify(exactly = 1) { cardScanner.scanAndOpenWallet(any(), any(), any()) }
        }

        @Test
        fun `GIVEN the launch scan WHEN it fails THEN the viewer stays on the welcome screen without a sheet`() {
            // Arrange
            val model = createModel(launchMode = InitScreenLaunchMode.WithCardScan)

            // Act
            model.capturedScanCallbacks().onFailed()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig).isNull()
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isNull()
            assertThat(sentEvents).containsExactly(SCREEN_OPENED)
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN the launch scan WHEN it leaves the screen THEN no sheet or story is touched`() {
            // Arrange
            val model = createModel(launchMode = InitScreenLaunchMode.WithCardScan)

            // Act
            model.capturedScanCallbacks().onNavigatedAway()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig).isNull()
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isNull()
            assertThat(sentEvents).containsExactly(SCREEN_OPENED)
        }

        @Test
        fun `GIVEN the launch scan is running WHEN create wallet clicked THEN the sheet opens as usual`() {
            // Arrange
            val model = createModel(launchMode = InitScreenLaunchMode.WithCardScan)

            // Act
            model.uiState.value.onCreateWalletClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class VideoRunning {

        @ParameterizedTest
        @ProvideTestModels
        fun setVideoRunning(model: RunningModel) {
            // Arrange
            val introductionModel = createModel()

            // Act
            introductionModel.setVideoRunning(model.isRunning)

            // Assert
            verify(exactly = 1) { videoPlayer.setRunning(model.isRunning) }
        }

        private fun provideTestModels() = listOf(
            RunningModel(isRunning = true),
            RunningModel(isRunning = false),
        )
    }

    @Test
    fun `GIVEN no frames coming out while playing WHEN the stall is noticed THEN a covered surface replaces it`() =
        runTest {
            // Arrange
            val model = createModel(dispatchers = createTestingCoroutineDispatcherProvider())
            surfaceAttached(isAttached = true)
            model.attachSurface(surfaceView)
            model.renderFrame()
            every { videoPlayer.outputFrameCount } returns 28L

            // Act
            model.onPlayingChanged(isPlaying = true)
            advanceTimeBy(STALL_DETECTED_AFTER_MS)
            runCurrent()

            // Assert
            assertThat(model.uiState.value.videoSurfaceKey).isEqualTo(1)
            assertThat(model.uiState.value.isVideoReady).isFalse()
            model.attachSurface(otherSurfaceView)
            model.renderFrame()
            assertThat(model.uiState.value.isVideoReady).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a stall new surfaces do not cure WHEN the watchdog gives up THEN the video is treated as failed`() =
        runTest {
            // Arrange
            val model = createModel(dispatchers = createTestingCoroutineDispatcherProvider())
            surfaceAttached(isAttached = true)
            model.attachSurface(surfaceView)
            model.renderFrame()
            every { videoPlayer.outputFrameCount } returns 28L

            // Act
            model.onPlayingChanged(isPlaying = true)
            advanceTimeBy(STALL_DETECTED_AFTER_MS * 4)
            runCurrent()

            // Assert
            assertThat(model.uiState.value.videoSurfaceKey).isEqualTo(3)
            assertThat(model.uiState.value.isVideoReady).isFalse()
            assertThat(model.uiState.value.isVideoFailed).isTrue()
            assertThat(model.uiState.value.isVideoRunning).isFalse()
            verify { videoPlayer.setRunning(isRunning = false) }
            assertThat(sentEvents.last()).isEqualTo(WelcomeMediaError(WelcomeMediaError.Stage.Playback))
            model.onDestroy()
        }

    @Test
    fun `GIVEN the video failed WHEN the screen resumes THEN playback stays stopped`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.renderFrame()
        model.onError(PLAYBACK_ERROR)

        // Act
        model.setVideoRunning(isRunning = true)

        // Assert
        verify(exactly = 0) { videoPlayer.setRunning(isRunning = true) }
        assertThat(model.uiState.value.isVideoRunning).isFalse()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateWalletSheet {

        @Test
        fun `GIVEN no sheet WHEN create wallet clicked THEN the sheet opens and the tap is reported`() {
            // Arrange
            val model = createModel()

            // Act
            model.uiState.value.onCreateWalletClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isTrue()
            assertThat(sentEvents)
                .containsExactly(
                    SCREEN_OPENED,
                    IntroductionProcess.ButtonCreateNewWallet(),
                    IntroductionProcess.CreateWalletIntroScreenOpened(screenType = null, referralId = null),
                )
                .inOrder()
        }

        @Test
        fun `GIVEN an open sheet WHEN create wallet clicked again THEN it stays the same sheet`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onCreateWalletClick()
            val opened = model.uiState.value.bottomSheetConfig

            // Act
            model.uiState.value.onCreateWalletClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig).isSameInstanceAs(opened)
            assertThat(sentEvents.filterIsInstance<IntroductionProcess.ButtonCreateNewWallet>()).hasSize(1)
        }

        @Test
        fun `GIVEN an open sheet WHEN dismissed THEN the content outlives the closing`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onCreateWalletClick()

            // Act
            model.closeCreateWalletSheet()

            // Assert
            val config = model.uiState.value.bottomSheetConfig
            assertThat(config?.isShown).isFalse()
            assertThat(config?.content).isInstanceOf(CreateWalletUM::class.java)
        }

        @Test
        fun `GIVEN a supported device WHEN the seed phrase option is picked THEN the wallet is created in place`() {
            // Arrange
            val model = createModel()

            // Act
            model.openSheet().seedPhrase.onClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isTrue()
            assertThat(model.seedPhraseRow().isLoading).isTrue()
            verify(exactly = 1) { trackingContextProxy.addHotWalletContext() }
            verify(exactly = 0) { trackingContextProxy.removeContext() }
            coVerify(exactly = 1) {
                mobileWalletCreator.createAndOpen(
                    source = AnalyticsParam.ScreensSources.CreateWalletIntro,
                    beforeOpen = any(),
                    onFailed = any(),
                )
            }
            verify { router wasNot Called }
            assertThat(sentEvents.last()).isEqualTo(
                OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet(
                    source = AnalyticsParam.ScreensSources.CreateWalletIntro.value,
                ),
            )
        }

        @Test
        fun `GIVEN the wallet is created WHEN it is about to open THEN the sheet collapses before the stack swaps`() =
            runTest {
                // Arrange
                val model = createModel()
                model.openSheet().seedPhrase.onClick()
                val beforeOpen = slot<suspend () -> Unit>()
                coVerify {
                    mobileWalletCreator.createAndOpen(
                        source = any(),
                        beforeOpen = capture(beforeOpen),
                        onFailed = any(),
                    )
                }

                // Act
                val settling = launch { beforeOpen.captured() }
                runCurrent()

                // Assert
                assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
                assertThat(settling.isCompleted).isFalse()

                model.uiState.value.onCreateWalletSheetCollapsed()
                runCurrent()
                assertThat(settling.isCompleted).isTrue()
            }

        @Test
        fun `GIVEN the wallet is being created WHEN the rows are tapped again THEN the taps are ignored`() {
            // Arrange
            val model = createModel()
            val sheet = model.openSheet()
            sheet.seedPhrase.onClick()

            // Act
            sheet.seedPhrase.onClick()
            sheet.cardOrRing.onClick()

            // Assert
            coVerify(exactly = 1) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isTrue()
            assertThat(model.seedPhraseRow().isLoading).isTrue()
            assertThat(model.cardOrRingRow().isEnabled).isFalse()
        }

        @Test
        fun `GIVEN the wallet is being created WHEN the sheet is closed and reopened THEN it still shows the creation`() {
            // Arrange
            coEvery { mobileWalletCreator.createAndOpen(any(), any(), any()) } coAnswers { awaitCancellation() }
            val model = createModel()
            model.openSheet().seedPhrase.onClick()
            model.closeCreateWalletSheet()

            // Act
            model.uiState.value.onCreateWalletClick()
            model.seedPhraseRow().onClick()
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.seedPhraseRow().isLoading).isTrue()
            assertThat(model.cardOrRingRow().isEnabled).isFalse()
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isNull()
            coVerify(exactly = 1) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the wallet is being created WHEN the sheet is closed and i have a wallet clicked THEN it stays closed`() {
            // Arrange
            coEvery { mobileWalletCreator.createAndOpen(any(), any(), any()) } coAnswers { awaitCancellation() }
            val model = createModel()
            model.openSheet().seedPhrase.onClick()
            model.closeCreateWalletSheet()

            // Act
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sheet closed during creation WHEN the creation fails THEN it stays closed with the rows released`() {
            // Arrange
            val model = createModel()
            model.openSheet().seedPhrase.onClick()
            val onFailed = slot<() -> Unit>()
            coVerify { mobileWalletCreator.createAndOpen(source = any(), beforeOpen = any(), onFailed = capture(onFailed)) }
            model.closeCreateWalletSheet()

            // Act
            onFailed.captured()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
            assertThat(model.seedPhraseRow().isLoading).isFalse()
            assertThat(model.cardOrRingRow().isEnabled).isTrue()
            model.uiState.value.onIHaveWalletClick()
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isTrue()
        }

        @Test
        fun `GIVEN the wallet creation failed WHEN reported THEN the row is released and the context dropped`() {
            // Arrange
            val model = createModel()
            model.openSheet().seedPhrase.onClick()
            val onFailed = slot<() -> Unit>()
            coVerify { mobileWalletCreator.createAndOpen(source = any(), beforeOpen = any(), onFailed = capture(onFailed)) }

            // Act
            onFailed.captured()

            // Assert
            assertThat(model.seedPhraseRow().isLoading).isFalse()
            assertThat(model.cardOrRingRow().isEnabled).isTrue()
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isTrue()
            verifyOrder {
                trackingContextProxy.addHotWalletContext()
                trackingContextProxy.removeContext()
            }
        }

        @Test
        fun `GIVEN an unsupported device WHEN the seed phrase option is picked THEN a warning replaces it`() {
            // Arrange
            every { isHotWalletCreationSupported() } returns false
            every { isHotWalletCreationSupported.getLeastVersionName() } returns LEAST_OS_VERSION
            val model = createModel()

            // Act
            model.openSheet().seedPhrase.onClick()

            // Assert
            val message = slot<DialogMessage>()
            verify(exactly = 1) { uiMessageSender.send(capture(message)) }
            assertThat(message.captured.title)
                .isEqualTo(hotWalletCreationNotSupportedDialog(LEAST_OS_VERSION).title)
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
            coVerify(exactly = 0) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            // Nothing downstream opens, so the context added for the tap has to be given back here.
            verifyOrder {
                trackingContextProxy.addHotWalletContext()
                trackingContextProxy.removeContext()
            }
        }

        @Test
        fun `GIVEN a dismissed sheet WHEN the seed phrase row is tapped during its collapse THEN it is inert`() {
            // Arrange
            val model = createModel()
            val sheet = model.openSheet()
            model.closeCreateWalletSheet()

            // Act
            sheet.seedPhrase.onClick()

            // Assert
            coVerify(exactly = 0) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            assertThat(sentEvents.filterIsInstance<OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet>())
                .isEmpty()
        }

        @Test
        fun `GIVEN a dismissed sheet WHEN the card row is tapped during its collapse THEN no story opens`() {
            // Arrange
            val model = createModel()
            val sheet = model.openSheet()
            model.setVideoRunning(isRunning = true)
            model.closeCreateWalletSheet()
            clearMocks(videoPlayer, answers = false)

            // Act
            sheet.cardOrRing.onClick()
            model.uiState.value.onCreateWalletSheetCollapsed()

            // Assert
            verify(exactly = 0) { videoPlayer.setRunning(isRunning = false) }
        }

        @Test
        fun `GIVEN an open sheet WHEN the card option is picked THEN the story waits for the collapse`() {
            // Arrange
            val model = createModel()
            model.setVideoRunning(isRunning = true)
            clearMocks(videoPlayer, answers = false)

            // Act
            model.openSheet().cardOrRing.onClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
            assertThat(model.uiState.value.isVideoRunning).isTrue()
            verify(exactly = 0) { videoPlayer.setRunning(isRunning = false) }
        }

        @Test
        fun `GIVEN the card option picked WHEN the sheet has collapsed THEN the story replaces the screen`() {
            // Arrange
            val model = createModel()
            model.setVideoRunning(isRunning = true)
            model.openSheet().cardOrRing.onClick()

            // Act
            model.uiState.value.onCreateWalletSheetCollapsed()

            // Assert
            assertThat(model.uiState.value.isVideoRunning).isFalse()
            verify(exactly = 1) { videoPlayer.setRunning(isRunning = false) }
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN no story asked for WHEN a dismissed sheet has collapsed THEN the screen keeps playing`() {
            // Arrange
            val model = createModel()
            model.setVideoRunning(isRunning = true)
            model.openSheet()
            model.closeCreateWalletSheet()
            clearMocks(videoPlayer, answers = false)

            // Act
            model.uiState.value.onCreateWalletSheetCollapsed()

            // Assert
            assertThat(model.uiState.value.isVideoRunning).isTrue()
            verify(exactly = 0) { videoPlayer.setRunning(isRunning = false) }
        }

        @Test
        fun `GIVEN the card option picked WHEN the buttons are tapped before the collapse THEN no sheet returns`() {
            // Arrange
            val model = createModel()
            model.openSheet().cardOrRing.onClick()

            // Act
            model.uiState.value.onCreateWalletClick()
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isNull()
        }

        @Test
        fun `GIVEN a dismissed sheet WHEN create wallet clicked again THEN it opens anew`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onCreateWalletClick()
            model.closeCreateWalletSheet()

            // Act
            model.uiState.value.onCreateWalletClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isTrue()
            assertThat(sentEvents.filterIsInstance<IntroductionProcess.ButtonCreateNewWallet>()).hasSize(2)
        }

        private fun IntroductionModel.openSheet(): CreateWalletUM {
            uiState.value.onCreateWalletClick()
            return uiState.value.bottomSheetConfig?.content as? CreateWalletUM
                ?: error("the Create wallet sheet is not open")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HaveWalletSheet {

        @Test
        fun `GIVEN no sheet WHEN i have a wallet clicked THEN the options open and the tap and opening are reported`() {
            // Arrange
            val model = createModel()

            // Act
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
            ).inOrder()
        }

        @Test
        fun `GIVEN an open sheet WHEN i have a wallet clicked again THEN it stays the same sheet reported once`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()
            val opened = model.uiState.value.haveWalletBottomSheetConfig

            // Act
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isSameInstanceAs(opened)
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
            ).inOrder()
        }

        @Test
        fun `GIVEN the options step WHEN import picked THEN the import step replaces it and the request is reported`() {
            // Arrange
            val model = createModel()

            // Act
            model.openHaveWallet().importWallet.onClick()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Import::class.java)
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isTrue()
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
                IntroductionProcess.ButtonImportExistingWallet(),
                OnboardingAnalyticsEvent.Backup.ImportWalletRequest(AnalyticsParam.CloudBackupAvailability.Available),
            ).inOrder()
        }

        @Test
        fun `GIVEN the import step WHEN back pressed THEN the options return and the sheet stays open`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            model.currentImportStep().onBack()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isTrue()
        }

        @Test
        fun `GIVEN an open sheet WHEN dismissed THEN the content outlives the closing`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()

            // Act
            model.closeHaveWalletSheet()

            // Assert
            val config = model.uiState.value.haveWalletBottomSheetConfig
            assertThat(config?.isShown).isFalse()
            assertThat(config?.content).isInstanceOf(HaveWalletUM::class.java)
        }

        @Test
        fun `GIVEN the options step WHEN sign in picked THEN nothing happens yet`() {
            // Arrange
            val model = createModel()
            val options = model.openHaveWallet()

            // Act
            // TODO TWI-1555: the row acts once social login lands.
            requireNotNull(options.socialLogin).onClick()

            // Assert
            verify { router wasNot Called }
            coVerify(exactly = 0) { cardScanner.scanAndOpenWallet(any(), any(), any()) }
        }

        @Test
        fun `GIVEN the options step WHEN card or ring picked THEN the sheet closes and the scan starts`() {
            // Arrange
            val model = createModel()

            // Act
            model.openHaveWallet().cardOrRing.onClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            assertThat(sentEvents.last()).isEqualTo(IntroductionProcess.ButtonSignInWithCard())
            assertThat(sentEvents.filterIsInstance<IntroductionProcess.ButtonScanCard>()).isEmpty()
            coVerify(exactly = 1) {
                cardScanner.scanAndOpenWallet(
                    analyticsSource = AnalyticsParam.ScreensSources.Intro,
                    onNavigatedAway = any(),
                    onFailed = any(),
                )
            }
        }

        @Test
        fun `GIVEN the scan started from the sheet WHEN it fails THEN no sheet returns and no opening is reported`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().cardOrRing.onClick()

            // Act
            model.capturedScanCallbacks().onFailed()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            assertThat(model.uiState.value.bottomSheetConfig).isNull()
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
                IntroductionProcess.ButtonSignInWithCard(),
            ).inOrder()
        }

        @Test
        fun `GIVEN a dismissed sheet WHEN the card row is tapped during its collapse THEN no scan starts`() {
            // Arrange
            val model = createModel()
            val options = model.openHaveWallet()
            model.closeHaveWalletSheet()

            // Act
            options.cardOrRing.onClick()

            // Assert
            coVerify(exactly = 0) { cardScanner.scanAndOpenWallet(any(), any(), any()) }
        }

        @Test
        fun `GIVEN an unsupported device WHEN recovery phrase picked THEN a warning replaces the sheet`() {
            // Arrange
            every { isHotWalletCreationSupported() } returns false
            every { isHotWalletCreationSupported.getLeastVersionName() } returns LEAST_OS_VERSION
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            model.currentImportStep().onRecoveryPhraseClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            val message = slot<DialogMessage>()
            verify(exactly = 1) { uiMessageSender.send(capture(message)) }
            assertThat(message.captured.title)
                .isEqualTo(hotWalletCreationNotSupportedDialog(LEAST_OS_VERSION).title)
            verify { router wasNot Called }
            verify(exactly = 0) { trackingContextProxy.addHotWalletContext() }
        }

        @Test
        fun `GIVEN the import step WHEN recovery phrase picked THEN the import flow opens`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            model.currentImportStep().onRecoveryPhraseClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            verify(exactly = 1) {
                router.push(
                    AppRoute.AddExistingWallet(
                        mode = AppRoute.AddExistingWallet.Mode.RecoveryPhrase,
                        shouldSendOnboardingStarted = true,
                    ),
                )
            }
            // The import flow owns the hot wallet analytics context; a second push here would overwrite its parent.
            verify(exactly = 0) { trackingContextProxy.addHotWalletContext() }
        }

        @Test
        fun `GIVEN backups found WHEN cloud backup picked THEN the restore opens`() {
            // Arrange
            coEvery { resolveCloudRestoreBackups() } returns CloudRestoreResult(listOf(BACKUP), "user@gmail.com").right()
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            model.currentImportStep().onCloudBackupClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            verify(exactly = 1) {
                router.push(
                    AppRoute.AddExistingWallet(
                        mode = AppRoute.AddExistingWallet.Mode.CloudRestore,
                        shouldSendOnboardingStarted = true,
                    ),
                )
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the listing fails WHEN cloud backup picked THEN the error step replaces the import step`(
            model: CloudFailureModel,
        ) {
            // Arrange
            coEvery { resolveCloudRestoreBackups() } returns model.error.left()
            val introductionModel = createModel()
            introductionModel.openHaveWallet().importWallet.onClick()

            // Act
            introductionModel.currentImportStep().onCloudBackupClick()

            // Assert
            val step = introductionModel.haveWalletStep() as HaveWalletUM.Step.Error
            assertThat(step.message).isEqualTo(requireNotNull(CloudRestoreErrorMessage.of(model.error)))
            assertThat(step.message.isWarning).isEqualTo(model.isWarning)
            assertThat(introductionModel.uiState.value.haveWalletBottomSheetConfig?.isShown).isTrue()
            verify { router wasNot Called }
        }

        private fun provideTestModels() = listOf(
            CloudFailureModel(error = CloudBackupError.BackupNotFound, isWarning = true),
            CloudFailureModel(error = CloudBackupError.AuthPermissionsMissing, isWarning = false),
            CloudFailureModel(error = CloudBackupError.NetworkError, isWarning = false),
        )

        @Test
        fun `GIVEN the account picker left WHEN cloud backup picked THEN the import step returns without loading`() {
            // Arrange
            coEvery { resolveCloudRestoreBackups() } returns CloudBackupError.AuthCanceled.left()
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            model.currentImportStep().onCloudBackupClick()

            // Assert
            assertThat(model.currentImportStep().isCloudLoading).isFalse()
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN the error step WHEN got it tapped THEN the whole sheet closes`() {
            // Arrange
            coEvery { resolveCloudRestoreBackups() } returns CloudBackupError.NetworkError.left()
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            model.currentImportStep().onCloudBackupClick()

            // Act
            (model.haveWalletStep() as HaveWalletUM.Step.Error).onGotItClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
        }

        @Test
        fun `GIVEN backups loading WHEN the buttons are tapped again THEN the taps are ignored`() {
            // Arrange
            coEvery { resolveCloudRestoreBackups() } coAnswers { awaitCancellation() }
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            model.currentImportStep().onCloudBackupClick()

            // Act
            model.currentImportStep().onCloudBackupClick()
            model.currentImportStep().onRecoveryPhraseClick()

            // Assert
            assertThat(model.currentImportStep().isCloudLoading).isTrue()
            coVerify(exactly = 1) { resolveCloudRestoreBackups() }
            verify { router wasNot Called }
            model.onDestroy()
        }

        @Test
        fun `GIVEN backups loading WHEN back pressed THEN the listing is dropped and a new one can start`() {
            // Arrange
            val listing = CompletableDeferred<Unit>()
            coEvery { resolveCloudRestoreBackups() } coAnswers {
                listing.await()
                CloudRestoreResult(listOf(BACKUP), null).right()
            }
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            model.currentImportStep().onCloudBackupClick()

            // Act
            model.currentImportStep().onBack()
            listing.complete(Unit)

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN the error step WHEN the outgoing import buttons are tapped THEN they are inert`() {
            // Arrange
            coEvery { resolveCloudRestoreBackups() } returns CloudBackupError.NetworkError.left()
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            val importStep = model.currentImportStep()
            importStep.onCloudBackupClick()

            // Act
            importStep.onRecoveryPhraseClick()
            importStep.onCloudBackupClick()
            importStep.onBack()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Error::class.java)
            coVerify(exactly = 1) { resolveCloudRestoreBackups() }
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN backups loading WHEN the sheet is closed THEN the listing is dropped`() {
            // Arrange
            val listing = CompletableDeferred<Unit>()
            coEvery { resolveCloudRestoreBackups() } coAnswers {
                listing.await()
                CloudRestoreResult(listOf(BACKUP), null).right()
            }
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            model.currentImportStep().onCloudBackupClick()

            // Act
            model.closeHaveWalletSheet()
            listing.complete(Unit)
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN the import step dismissed WHEN back pressed during the collapse THEN the sheet stays closed`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            val step = model.currentImportStep()
            model.closeHaveWalletSheet()

            // Act
            step.onBack()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
        }

        @Test
        fun `GIVEN an unsupported device WHEN cloud backup picked THEN a warning replaces the sheet before any listing`() {
            // Arrange
            every { isHotWalletCreationSupported() } returns false
            every { isHotWalletCreationSupported.getLeastVersionName() } returns LEAST_OS_VERSION
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            model.currentImportStep().onCloudBackupClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            coVerify(exactly = 0) { resolveCloudRestoreBackups() }
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN cloud backups unavailable WHEN the import step opens THEN the cloud button is absent and inert`() {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns false
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            model.currentImportStep().onCloudBackupClick()

            // Assert
            assertThat(model.currentImportStep().cloudBackupButtonText).isNull()
            coVerify(exactly = 0) { resolveCloudRestoreBackups() }
        }

        @Test
        fun `GIVEN the import step dismissed WHEN recovery phrase tapped during the collapse THEN it is inert`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            val step = model.currentImportStep()
            model.closeHaveWalletSheet()

            // Act
            step.onRecoveryPhraseClick()

            // Assert
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN the sheet dismissed on the import step WHEN reopened THEN it starts from the options again`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            model.closeHaveWalletSheet()

            // Act
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isTrue()
        }

        @Test
        fun `GIVEN the import step WHEN closed THEN the whole sheet goes away`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            model.closeHaveWalletSheet()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
        }

        @Test
        fun `GIVEN the import step WHEN import picked again THEN the step is not rebuilt`() {
            // Arrange
            val model = createModel()
            val options = model.openHaveWallet()
            options.importWallet.onClick()
            val step = model.haveWalletStep()

            // Act
            options.importWallet.onClick()

            // Assert
            assertThat(model.haveWalletStep()).isSameInstanceAs(step)
        }

        @Test
        fun `GIVEN social login disabled WHEN the options open THEN the sign in row is absent`() {
            // Arrange
            every { socialLoginFeatureToggles.isSocialLoginEnabled } returns false
            val model = createModel()

            // Act
            val options = model.openHaveWallet()

            // Assert
            assertThat(options.socialLogin).isNull()
        }

        @Test
        fun `GIVEN the options step WHEN rendered THEN every row carries its own copy`() {
            // Arrange
            val model = createModel()

            // Act
            val options = model.openHaveWallet()

            // Assert
            assertThat(options.cardOrRing.title)
                .isEqualTo(resourceReference(CoreResR.string.introduction_create_wallet_hardware_title))
            assertThat(options.cardOrRing.description)
                .isEqualTo(resourceReference(CoreResR.string.introduction_i_have_wallet_hardware_description))
            assertThat(options.socialLogin?.title)
                .isEqualTo(resourceReference(CoreResR.string.introduction_i_have_wallet_sign_in_google_title))
            assertThat(options.importWallet.title)
                .isEqualTo(resourceReference(CoreResR.string.hw_import_existing_wallet))
            assertThat(options.importWallet.description)
                .isEqualTo(resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description))
        }

        private fun IntroductionModel.openHaveWallet(): HaveWalletUM.Step.Options {
            uiState.value.onIHaveWalletClick()
            return haveWalletStep() as? HaveWalletUM.Step.Options
                ?: error("the sheet is not on the Options step")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnboardingStories {

        @Test
        fun `GIVEN the story shown WHEN the screen resumes THEN the background video stays paused`() {
            // Arrange
            val model = createModel()
            model.openStory()
            clearMocks(videoPlayer, answers = false)

            // Act
            model.setVideoRunning(isRunning = true)

            // Assert
            verify(exactly = 1) { videoPlayer.setRunning(isRunning = false) }
            verify(exactly = 0) { videoPlayer.setRunning(isRunning = true) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the story shown WHEN it ends THEN the welcome returns without a sheet and the video goes`(
            model: StoryReturnModel,
        ) {
            // Arrange
            val introductionModel = createModel()
            introductionModel.openStory()
            clearMocks(videoPlayer, answers = false)

            // Act
            introductionModel.onStoryResult(model.result)

            // Assert
            assertThat(introductionModel.uiState.value.bottomSheetConfig?.isShown).isNotEqualTo(true)
            verify(exactly = 1) { videoPlayer.setRunning(isRunning = true) }
            verify(exactly = 0) { videoPlayer.restart() }
        }

        private fun provideTestModels() = listOf(
            StoryReturnModel(name = "dismissed", result = StoriesV2Result.Dismissed),
            StoryReturnModel(name = "completed", result = StoriesV2Result.Completed),
            StoryReturnModel(
                name = "no content",
                result = StoriesV2Result.Unavailable(StoriesV2Result.Unavailable.Reason.NO_CONTENT),
            ),
        )

        @Test
        fun `GIVEN the story shown WHEN the buy action is picked THEN the link opens and the story is kept`() {
            // Arrange
            val model = createModel()
            model.openStory()
            coEvery {
                generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Creation)
            } returns BUY_LINK
            val action = pendingAction(actionId = "learn_more")
            sentEvents.clear()

            // Act
            model.onStoryAction(action)

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(BUY_LINK) }
            verify(exactly = 0) { action.end() }
            verify(exactly = 0) { action.resume() }
            assertThat(sentEvents).containsExactly(
                Basic.ButtonBuy(source = AnalyticsParam.ScreensSources.Stories),
                Shop.ScreenOpened(),
            ).inOrder()
        }

        @Test
        fun `GIVEN the store opened from a slide WHEN the screen resumes THEN that slide carries on`() {
            // Arrange
            val model = createModel()
            model.openStory()
            coEvery {
                generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Creation)
            } returns BUY_LINK
            val action = pendingAction(actionId = "learn_more")
            model.onStoryAction(action)
            model.setVideoRunning(isRunning = false)

            // Act
            model.setVideoRunning(isRunning = true)
            model.setVideoRunning(isRunning = false)
            model.setVideoRunning(isRunning = true)

            // Assert
            verify(exactly = 1) { action.resume() }
            verify(exactly = 0) { action.end() }
            assertThat(model.uiState.value.isVideoRunning).isFalse()
        }

        @Test
        fun `GIVEN the store opened from a slide WHEN the viewer closes the story THEN the resume settles nothing`() {
            // Arrange
            val model = createModel()
            model.openStory()
            coEvery {
                generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Creation)
            } returns BUY_LINK
            val action = pendingAction(actionId = "learn_more")
            model.onStoryAction(action)
            model.onStoryResult(StoriesV2Result.Dismissed)
            model.setVideoRunning(isRunning = false)

            // Act
            model.setVideoRunning(isRunning = true)

            // Assert
            verify(exactly = 0) { action.resume() }
            verify(exactly = 0) { action.end() }
        }

        @Test
        fun `GIVEN the story shown WHEN the scan action is picked THEN the scan starts from the intro`() {
            // Arrange
            val model = createModel()
            model.openStory()

            // Act
            model.onStoryAction(pendingAction(actionId = "scan_wallet"))

            // Assert
            coVerify(exactly = 1) {
                cardScanner.scanAndOpenWallet(
                    analyticsSource = AnalyticsParam.ScreensSources.Intro,
                    onNavigatedAway = any(),
                    onFailed = any(),
                )
            }
            assertThat(sentEvents.last())
                .isEqualTo(IntroductionProcess.ButtonScanCard(AnalyticsParam.ScreensSources.Intro))
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
        }

        @Test
        fun `GIVEN the card row tapped WHEN the composition is unavailable THEN the scan starts from the intro`() {
            // Arrange
            val model = createModel()
            model.openStory()

            // Act
            model.onStoryResult(StoriesV2Result.Unavailable(StoriesV2Result.Unavailable.Reason.NO_CONTENT))

            // Assert
            coVerify(exactly = 1) {
                cardScanner.scanAndOpenWallet(
                    analyticsSource = AnalyticsParam.ScreensSources.Intro,
                    onNavigatedAway = any(),
                    onFailed = any(),
                )
            }
            assertThat(sentEvents.last())
                .isEqualTo(IntroductionProcess.ButtonScanCard(AnalyticsParam.ScreensSources.Intro))
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
        }

        @Test
        fun `GIVEN a scan in flight WHEN the composition turns unavailable THEN the gone story is not resumed`() {
            // Arrange
            val model = createModel()
            val action = pendingAction(actionId = "scan_wallet")
            model.openStory()
            model.onStoryAction(action)
            model.onStoryResult(StoriesV2Result.Unavailable(StoriesV2Result.Unavailable.Reason.NO_USABLE_FRAME))

            // Act
            model.capturedScanCallbacks().onFailed()

            // Assert
            verify(exactly = 0) { action.resume() }
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isNotEqualTo(true)
        }

        @Test
        fun `GIVEN the scan suspended the story WHEN the scan fails THEN the story resumes on the same slide`() {
            // Arrange
            val model = createModel()
            model.openStory()
            val action = pendingAction(actionId = "scan_wallet")
            model.onStoryAction(action)

            // Act
            model.capturedScanCallbacks().onFailed()

            // Assert
            verify(exactly = 1) { action.resume() }
            verify(exactly = 0) { action.end() }
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
        }

        @Test
        fun `GIVEN the story closed during a scan WHEN the scan fails THEN the welcome stays without a sheet`() {
            // Arrange
            val model = createModel()
            model.openStory()
            val action = pendingAction(actionId = "scan_wallet")
            model.onStoryAction(action)
            model.onStoryResult(StoriesV2Result.Dismissed)

            // Act
            model.capturedScanCallbacks().onFailed()

            // Assert
            verify(exactly = 0) { action.resume() }
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isNotEqualTo(true)
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isNull()
        }

        @Test
        fun `GIVEN the scan leaves the screen WHEN navigation covers the story THEN it ends without a teardown`() {
            // Arrange
            val model = createModel()
            model.openStory()
            val action = pendingAction(actionId = "scan_wallet")
            model.onStoryAction(action)

            // Act
            model.capturedScanCallbacks().onNavigatedAway()

            // Assert
            verify(exactly = 1) { action.end() }
        }

        @Test
        fun `GIVEN a story ended by the scan WHEN the screen resumes THEN the player goes and no sheet returns`() {
            // Arrange
            val model = createModel()
            model.openStory()
            model.onStoryAction(pendingAction(actionId = "scan_wallet"))
            model.capturedScanCallbacks().onNavigatedAway()
            // What the live player does inside end(): the terminal result arrives before the navigation lands.
            model.onStoryResult(
                StoriesV2Result.ActionInvoked(
                    actionId = "scan_wallet",
                    target = StoryV2ActionTarget.Screen(key = "scan_wallet"),
                ),
            )
            clearMocks(videoPlayer, answers = false)

            // Act
            model.setVideoRunning(isRunning = true)

            // Assert
            verify(exactly = 1) { videoPlayer.setRunning(isRunning = true) }
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
        }

        @Test
        fun `GIVEN a paused screen WHEN the story returns THEN the background video stays paused`() {
            // Arrange
            val model = createModel()
            model.openStory()
            model.setVideoRunning(isRunning = false)
            clearMocks(videoPlayer, answers = false)

            // Act
            model.onStoryResult(StoriesV2Result.Dismissed)

            // Assert
            verify(exactly = 1) { videoPlayer.setRunning(isRunning = false) }
            verify(exactly = 0) { videoPlayer.setRunning(isRunning = true) }
        }

        @Test
        fun `GIVEN the story shown WHEN an unknown action arrives THEN it resumes on the same slide`() {
            // Arrange
            val model = createModel()
            model.openStory()
            val action = pendingAction(actionId = "some_future_action")

            // Act
            model.onStoryAction(action)

            // Assert
            verify(exactly = 1) { action.resume() }
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN the story closed WHEN the screen pauses and resumes THEN the video keeps its position`() {
            // Arrange
            val model = createModel()
            model.openStory()
            model.onStoryResult(StoriesV2Result.Dismissed)

            // Act
            model.setVideoRunning(isRunning = false)
            model.setVideoRunning(isRunning = true)

            // Assert
            verify(exactly = 0) { videoPlayer.restart() }
        }

        @Test
        fun `GIVEN the scan left the screen WHEN the screen resumes THEN the video restarts from the beginning`() {
            // Arrange
            val model = createModel()
            model.openStory()
            model.onStoryAction(pendingAction(actionId = "scan_wallet"))
            model.capturedScanCallbacks().onNavigatedAway()
            model.setVideoRunning(isRunning = false)

            // Act
            model.setVideoRunning(isRunning = true)
            model.setVideoRunning(isRunning = false)
            model.setVideoRunning(isRunning = true)

            // Assert
            verify(exactly = 1) { videoPlayer.restart() }
        }

        @Test
        fun `GIVEN the recovery phrase import left the screen WHEN the screen resumes THEN the video restarts`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()
            model.currentImportStep().onRecoveryPhraseClick()
            model.setVideoRunning(isRunning = false)

            // Act
            model.setVideoRunning(isRunning = true)

            // Assert
            verify(exactly = 1) { videoPlayer.restart() }
            verify(exactly = 1) {
                router.push(
                    AppRoute.AddExistingWallet(
                        mode = AppRoute.AddExistingWallet.Mode.RecoveryPhrase,
                        shouldSendOnboardingStarted = true,
                    ),
                )
            }
        }

        private fun pendingAction(actionId: String): StoriesV2PendingAction {
            return mockk(relaxed = true) { every { this@mockk.actionId } returns actionId }
        }

        private fun IntroductionModel.openStory() {
            setVideoRunning(isRunning = true)
            openSheet().cardOrRing.onClick()
            // What the screen reports once the dismissed sheet has finished folding away.
            uiState.value.onCreateWalletSheetCollapsed()
        }

        private fun IntroductionModel.openSheet(): CreateWalletUM {
            uiState.value.onCreateWalletClick()
            return uiState.value.bottomSheetConfig?.content as? CreateWalletUM
                ?: error("the Create wallet sheet is not open")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SheetAnalytics {

        @Test
        fun `GIVEN a referral WHEN create wallet clicked THEN the sheet opening carries it without a screen type`() {
            // Arrange
            coEvery { appsFlyerRepository.getConversionData() } returns AppsFlyerConversionData("ref-42", null)
            val model = createModel()

            // Act
            model.uiState.value.onCreateWalletClick()

            // Assert
            assertThat(sentEvents.last()).isEqualTo(
                IntroductionProcess.CreateWalletIntroScreenOpened(screenType = null, referralId = "ref-42"),
            )
        }

        @Test
        fun `GIVEN a referral WHEN i have a wallet clicked THEN the sheet opening is sent without params`() {
            // Arrange
            coEvery { appsFlyerRepository.getConversionData() } returns AppsFlyerConversionData("ref-42", null)
            val model = createModel()

            // Act
            model.uiState.value.onIHaveWalletClick()

            // Assert
            val openings = sentEvents.filterIsInstance<IntroductionProcess.IHaveWalletIntroScreenOpened>()
            assertThat(openings.map { it.id to it.params }).containsExactly(
                "[Introduction Process] I Have Wallet Intro Screen Opened" to emptyMap<String, String>(),
            )
        }

        @Test
        fun `GIVEN the create sheet WHEN the card row is picked THEN the hardware choice is reported`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onCreateWalletClick()

            // Act
            model.createSheet().cardOrRing.onClick()

            // Assert
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonCreateNewWallet(),
                IntroductionProcess.CreateWalletIntroScreenOpened(screenType = null, referralId = null),
                IntroductionProcess.ButtonHardwareWallet(),
            ).inOrder()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the create sheet WHEN the viewer leaves it THEN the dismissal is reported once`(
            model: DismissModel,
        ) {
            // Arrange
            val introductionModel = createModel()
            introductionModel.uiState.value.onCreateWalletClick()

            // Act
            model.leave(introductionModel)
            model.leave(introductionModel)

            // Assert
            assertThat(introductionModel.uiState.value.bottomSheetConfig?.isShown).isFalse()
            assertThat(introductionModel.uiState.value.bottomSheetConfig).isNotNull()
            assertThat(sentEvents.filterIsInstance<SheetDismissed>()).containsExactly(
                SheetDismissed(SheetDismissed.Sheet.CreateWallet, model.reason),
            )
        }

        @Test
        fun `GIVEN the have wallet options WHEN closed by the button THEN the dismissal names the sheet`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()

            // Act
            model.haveWalletContent().onCloseClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
                SheetDismissed(SheetDismissed.Sheet.IHaveWallet, SheetDismissed.DismissReason.CloseButton),
            ).inOrder()
        }

        @ParameterizedTest
        @MethodSource("provideHaveWalletDismissModels")
        fun `GIVEN the have wallet options left WHEN i have a wallet clicked again THEN the opening is reported again`(
            model: DismissModel,
        ) {
            // Arrange
            val introductionModel = createModel()
            introductionModel.uiState.value.onIHaveWalletClick()
            model.leave(introductionModel)

            // Act
            introductionModel.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(introductionModel.uiState.value.haveWalletBottomSheetConfig?.isShown).isTrue()
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
                SheetDismissed(SheetDismissed.Sheet.IHaveWallet, model.reason),
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
            ).inOrder()
        }

        @Test
        fun `GIVEN cloud backups switched off WHEN import picked THEN the request reports them unavailable`() {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns false
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()

            // Act
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()

            // Assert
            assertThat(sentEvents.last()).isEqualTo(
                OnboardingAnalyticsEvent.Backup.ImportWalletRequest(AnalyticsParam.CloudBackupAvailability.Unavailable),
            )
        }

        @Test
        fun `GIVEN the import step WHEN closed by the button THEN the dismissal names the import sheet`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()

            // Act
            model.haveWalletContent().onCloseClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            assertThat(sentEvents.last()).isEqualTo(
                SheetDismissed(SheetDismissed.Sheet.ImportExisting, SheetDismissed.DismissReason.CloseButton),
            )
        }

        @Test
        fun `GIVEN the cloud restore failed WHEN got it is tapped THEN the import sheet reports a close`() {
            // Arrange
            coEvery { resolveCloudRestoreBackups() } returns CloudBackupError.BackupNotFound.left()
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()
            model.currentImportStep().onCloudBackupClick()

            // Act
            (model.haveWalletStep() as HaveWalletUM.Step.Error).onGotItClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            assertThat(sentEvents.last()).isEqualTo(
                SheetDismissed(SheetDismissed.Sheet.ImportExisting, SheetDismissed.DismissReason.CloseButton),
            )
        }

        @Test
        fun `GIVEN the import step WHEN dismissed from outside THEN the dismissal names the import sheet`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()

            // Act
            model.closeHaveWalletSheet()
            model.closeHaveWalletSheet()

            // Assert
            assertThat(sentEvents.filterIsInstance<SheetDismissed>()).containsExactly(
                SheetDismissed(SheetDismissed.Sheet.ImportExisting, SheetDismissed.DismissReason.Dismissed),
            )
        }

        @Test
        fun `GIVEN the import step WHEN back pressed THEN only the back is reported and the options return`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()

            // Act
            model.currentImportStep().onBack()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
                IntroductionProcess.ButtonImportExistingWallet(),
                OnboardingAnalyticsEvent.Backup.ImportWalletRequest(AnalyticsParam.CloudBackupAvailability.Available),
                SheetDismissed(SheetDismissed.Sheet.ImportExisting, SheetDismissed.DismissReason.BackButton),
            ).inOrder()
        }

        @Test
        fun `GIVEN the options step WHEN import picked THEN the import event carries the mobile wallet product type`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()

            // Act
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()

            // Assert
            assertThat(
                sentEvents.filterIsInstance<IntroductionProcess.ButtonImportExistingWallet>().map { it.id to it.params },
            ).containsExactly(
                "[Introduction Process] Button - Import Existing Wallet" to mapOf("Product Type" to "Mobile Wallet"),
            )
        }

        @Test
        fun `GIVEN the import step left by back WHEN import picked again THEN the import is reported again`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()
            model.currentImportStep().onBack()

            // Act
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()

            // Assert
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
                IntroductionProcess.ButtonImportExistingWallet(),
                OnboardingAnalyticsEvent.Backup.ImportWalletRequest(AnalyticsParam.CloudBackupAvailability.Available),
                SheetDismissed(SheetDismissed.Sheet.ImportExisting, SheetDismissed.DismissReason.BackButton),
                IntroductionProcess.ButtonImportExistingWallet(),
                OnboardingAnalyticsEvent.Backup.ImportWalletRequest(AnalyticsParam.CloudBackupAvailability.Available),
            ).inOrder()
        }

        @Test
        fun `GIVEN an unsupported device WHEN import picked THEN the import is still reported`() {
            // Arrange
            every { isHotWalletCreationSupported() } returns false
            every { isHotWalletCreationSupported.getLeastVersionName() } returns LEAST_OS_VERSION
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()

            // Act
            (model.haveWalletStep() as HaveWalletUM.Step.Options).importWallet.onClick()

            // Assert
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonIHaveWallet(),
                IntroductionProcess.IHaveWalletIntroScreenOpened(),
                IntroductionProcess.ButtonImportExistingWallet(),
                OnboardingAnalyticsEvent.Backup.ImportWalletRequest(AnalyticsParam.CloudBackupAvailability.Available),
            ).inOrder()
        }

        @Test
        fun `GIVEN the have wallet options WHEN the card row is picked THEN no dismissal is reported`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()

            // Act
            (model.haveWalletStep() as HaveWalletUM.Step.Options).cardOrRing.onClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isFalse()
            assertThat(sentEvents.filterIsInstance<SheetDismissed>()).isEmpty()
        }

        private fun IntroductionModel.createSheet(): CreateWalletUM =
            uiState.value.bottomSheetConfig?.content as CreateWalletUM

        private fun IntroductionModel.haveWalletContent(): HaveWalletUM =
            uiState.value.haveWalletBottomSheetConfig?.content as HaveWalletUM

        private fun provideTestModels() = listOf(
            DismissModel(reason = SheetDismissed.DismissReason.CloseButton) { it.createSheet().onCloseClick() },
            DismissModel(reason = SheetDismissed.DismissReason.Dismissed) { it.closeCreateWalletSheet() },
        )

        private fun provideHaveWalletDismissModels() = listOf(
            DismissModel(reason = SheetDismissed.DismissReason.CloseButton) { it.haveWalletContent().onCloseClick() },
            DismissModel(reason = SheetDismissed.DismissReason.Dismissed) { it.closeHaveWalletSheet() },
        )
    }

    internal class DismissModel(val reason: SheetDismissed.DismissReason, val leave: (IntroductionModel) -> Unit) {
        override fun toString() = reason.name
    }

    internal data class ScanCallbacks(val onNavigatedAway: () -> Unit, val onFailed: () -> Unit)

    internal data class CloudFailureModel(val error: CloudBackupError, val isWarning: Boolean)

    internal data class StoryReturnModel(val name: String, val result: StoriesV2Result) {
        override fun toString(): String = name
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SheetExclusion {

        @Test
        fun `GIVEN the create wallet sheet open WHEN i have a wallet clicked THEN the second sheet stays closed`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onCreateWalletClick()

            // Act
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isNull()
            assertThat(sentEvents.filterIsInstance<IntroductionProcess.IHaveWalletIntroScreenOpened>()).isEmpty()
        }

        @Test
        fun `GIVEN the have wallet sheet open WHEN create wallet clicked THEN the second sheet stays closed`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()

            // Act
            model.uiState.value.onCreateWalletClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig).isNull()
            assertThat(sentEvents.filterIsInstance<IntroductionProcess.ButtonCreateNewWallet>()).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BuyTangem {

        @BeforeEach
        fun stubWelcomeLink() {
            coEvery {
                generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Welcome)
            } returns BUY_LINK
        }

        @Test
        fun `GIVEN the idle welcome screen WHEN buy tangem clicked THEN the welcome store opens with its events`() {
            // Arrange
            val model = createModel()

            // Act
            model.uiState.value.onBuyTangemClick()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(BUY_LINK) }
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonBuyTangem(),
                Shop.ScreenOpened(),
            ).inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the store open WHEN buy tangem clicked again THEN it opens once`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onBuyTangemClick()

            // Act
            model.uiState.value.onBuyTangemClick()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(BUY_LINK) }
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonBuyTangem(),
                Shop.ScreenOpened(),
            ).inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the link still being built WHEN buy tangem clicked again THEN it opens once`() {
            // Arrange
            val link = CompletableDeferred<String>()
            coEvery {
                generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Welcome)
            } coAnswers { link.await() }
            val model = createModel()
            model.uiState.value.onBuyTangemClick()

            // Act
            model.uiState.value.onBuyTangemClick()
            link.complete(BUY_LINK)

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(BUY_LINK) }
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonBuyTangem(),
                Shop.ScreenOpened(),
            ).inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the store closed WHEN buy tangem clicked again THEN it opens again`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onBuyTangemClick()
            model.setVideoRunning(isRunning = false)
            model.setVideoRunning(isRunning = true)

            // Act
            model.uiState.value.onBuyTangemClick()

            // Assert
            verify(exactly = 2) { urlOpener.openUrl(BUY_LINK) }
            assertThat(sentEvents).containsExactly(
                SCREEN_OPENED,
                IntroductionProcess.ButtonBuyTangem(),
                Shop.ScreenOpened(),
                IntroductionProcess.ButtonBuyTangem(),
                Shop.ScreenOpened(),
            ).inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN no pause after the tap WHEN the cover timeout passes THEN it opens again`() = runTest {
            // Arrange
            val model = createModel(dispatchers = createTestingCoroutineDispatcherProvider())
            runCurrent()
            model.uiState.value.onBuyTangemClick()
            runCurrent()
            advanceTimeBy(IntroductionStore.BROWSER_COVER_TIMEOUT)
            runCurrent()

            // Act
            model.uiState.value.onBuyTangemClick()
            runCurrent()

            // Assert
            verify(exactly = 2) { urlOpener.openUrl(BUY_LINK) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the link failing to build WHEN buy tangem clicked again THEN it tries again`() {
            // Arrange
            coEvery {
                generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Welcome)
            } throws IllegalStateException("corrupted referral") andThen BUY_LINK
            val model = createModel()
            model.uiState.value.onBuyTangemClick()

            // Act
            model.uiState.value.onBuyTangemClick()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(BUY_LINK) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN a card scan ended WHEN buy tangem clicked THEN the store opens`() {
            // Arrange
            val scan = CompletableDeferred<Unit>()
            coEvery { cardScanner.scanAndOpenWallet(any(), any(), any()) } coAnswers { scan.await() }
            val model = createModel(launchMode = InitScreenLaunchMode.WithCardScan)
            scan.complete(Unit)

            // Act
            model.uiState.value.onBuyTangemClick()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(BUY_LINK) }
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the screen busy WHEN buy tangem clicked THEN nothing opens and nothing is sent`(
            model: BusyScreenModel,
        ) {
            // Arrange
            val introductionModel = model.arrange()
            sentEvents.clear()

            // Act
            introductionModel.uiState.value.onBuyTangemClick()

            // Assert
            verify(exactly = 0) { urlOpener.openUrl(any()) }
            assertThat(sentEvents).isEmpty()
            introductionModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            BusyScreenModel(name = "create wallet sheet open") {
                createModel().apply { uiState.value.onCreateWalletClick() }
            },
            BusyScreenModel(name = "have wallet sheet open") {
                createModel().apply { uiState.value.onIHaveWalletClick() }
            },
            BusyScreenModel(name = "story opening") {
                createModel().apply {
                    uiState.value.onCreateWalletClick()
                    cardOrRingRow().onClick()
                }
            },
            BusyScreenModel(name = "story shown") {
                createModel().apply {
                    uiState.value.onCreateWalletClick()
                    cardOrRingRow().onClick()
                    uiState.value.onCreateWalletSheetCollapsed()
                }
            },
            BusyScreenModel(name = "mobile wallet being created") {
                coEvery { mobileWalletCreator.createAndOpen(any(), any(), any()) } coAnswers { awaitCancellation() }
                createModel().apply {
                    uiState.value.onCreateWalletClick()
                    seedPhraseRow().onClick()
                    closeCreateWalletSheet()
                }
            },
            BusyScreenModel(name = "card being scanned") {
                coEvery { cardScanner.scanAndOpenWallet(any(), any(), any()) } coAnswers { awaitCancellation() }
                createModel(launchMode = InitScreenLaunchMode.WithCardScan)
            },
        )
    }

    internal data class BusyScreenModel(val name: String, val arrange: () -> IntroductionModel) {
        override fun toString(): String = name
    }

    /** Callbacks of the latest scan: a fallback scan can follow the one a story action started. */
    private fun IntroductionModel.capturedScanCallbacks(): ScanCallbacks {
        val onNavigatedAway = mutableListOf<() -> Unit>()
        val onFailed = mutableListOf<() -> Unit>()
        coVerify {
            cardScanner.scanAndOpenWallet(
                analyticsSource = any(),
                onNavigatedAway = capture(onNavigatedAway),
                onFailed = capture(onFailed),
            )
        }
        return ScanCallbacks(onNavigatedAway.last(), onFailed.last())
    }

    private fun IntroductionModel.seedPhraseRow(): WalletOptionUM =
        (uiState.value.bottomSheetConfig?.content as CreateWalletUM).seedPhrase

    private fun IntroductionModel.cardOrRingRow(): WalletOptionUM =
        (uiState.value.bottomSheetConfig?.content as CreateWalletUM).cardOrRing

    private fun IntroductionModel.haveWalletStep(): HaveWalletUM.Step? =
        (uiState.value.haveWalletBottomSheetConfig?.content as? HaveWalletUM)?.step

    private fun IntroductionModel.currentImportStep(): HaveWalletUM.Step.Import =
        haveWalletStep() as? HaveWalletUM.Step.Import ?: error("the sheet is not on the Import step")

    private fun IntroductionModel.closeCreateWalletSheet() {
        uiState.value.bottomSheetConfig?.onDismissRequest?.invoke()
    }

    private fun IntroductionModel.closeHaveWalletSheet() {
        uiState.value.haveWalletBottomSheetConfig?.onDismissRequest?.invoke()
    }

    internal data class RunningModel(val isRunning: Boolean)

    private fun IntroductionModel.renderFrame() {
        onFirstFrameRendered()
    }

    private fun IntroductionUM.renderedFields() = isVideoReady to isMotionEnabled

    private fun surfaceAttached(isAttached: Boolean) {
        every { videoPlayer.isSurfaceAttached } returns isAttached
    }

    private fun createModel(
        launchMode: InitScreenLaunchMode = InitScreenLaunchMode.Standard,
        entryReason: AppRoute.Home.EntryReason = AppRoute.Home.EntryReason.Launch,
        dispatchers: CoroutineDispatcherProvider = TestingCoroutineDispatcherProvider(),
    ) = IntroductionModel(
        dispatchers = dispatchers,
        paramsContainer = MutableParamsContainer(IntroductionComponent.Params(launchMode, entryReason)),
        videoPlayerFactory = videoPlayerFactory,
        urlOpener = urlOpener,
        analyticsEventHandler = analyticsEventHandler,
        entryReasonTracker = entryReasonTracker,
        router = router,
        isHotWalletCreationSupported = isHotWalletCreationSupported,
        socialLoginFeatureToggles = socialLoginFeatureToggles,
        hotWalletFeatureToggles = hotWalletFeatureToggles,
        resolveCloudRestoreBackups = resolveCloudRestoreBackups,
        trackingContextProxy = trackingContextProxy,
        cardScanner = cardScanner,
        mobileWalletCreator = mobileWalletCreator,
        referral = IntroductionReferral(appsFlyerRepository),
        legalLinks = IntroductionLegalLinks(urlOpener, analyticsEventHandler),
        generateBuyTangemCardLinkUseCase = generateBuyTangemCardLinkUseCase,
        uiMessageSender = uiMessageSender,
    )

    private companion object {
        val PLAYBACK_ERROR = IllegalStateException("decoder failed")
        val VIDEO_SIZE = IntSize(width = 1080, height = 1920)

        val SCREEN_OPENED = IntroductionProcess.ScreenOpened(IntroductionProcess.ScreenOpened.EntryReason.NoWallets)

        /** The first check only takes the baseline, then four more checks 250 ms apart see the count stand still. */
        const val STALL_DETECTED_AFTER_MS = 1_251L
        val SHUTTER_DOWN = false to true
        const val BUY_LINK = "https://buy.tangem.com/en?utm_source=tangem-app"
        const val LEAST_OS_VERSION = "Android 10"
        val BACKUP = CloudBackupInfo(
            fileId = "file-1",
            backupName = "My Wallet",
            walletName = "My Wallet",
            createdAtMillis = 0L,
            walletId = "011",
        )
    }
}