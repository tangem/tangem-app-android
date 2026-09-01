package com.tangem.features.introduction.impl.model

import android.view.SurfaceView
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionModelTest {

    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val videoPlayer: IntroductionVideoPlayer = mockk(relaxed = true)
    private val videoPlayerFactory: IntroductionVideoPlayer.Factory = mockk(relaxed = true)
    private val surfaceView: SurfaceView = mockk(relaxed = true)
    private val otherSurfaceView: SurfaceView = mockk(relaxed = true)

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(urlOpener, analyticsEventHandler, videoPlayer, videoPlayerFactory)
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
        model.onFirstFrameRendered()
        assertThat(model.uiState.value.isVideoReady).isTrue()

        surfaceAttached(isAttached = false)
        model.detachSurface(surfaceView)
        assertThat(model.uiState.value.isVideoReady).isFalse()

        surfaceAttached(isAttached = true)
        model.attachSurface(otherSurfaceView)
        assertThat(model.uiState.value.isVideoReady).isFalse()

        model.onFirstFrameRendered()
        assertThat(model.uiState.value.isVideoReady).isTrue()
    }

    @Test
    fun `GIVEN a replacement surface WHEN the previous one is released THEN the picture stays uncovered`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.onFirstFrameRendered()
        model.attachSurface(otherSurfaceView)

        // Act
        model.detachSurface(surfaceView)

        // Assert
        assertThat(model.uiState.value.isVideoReady).isTrue()
        model.onError()
        assertThat(model.uiState.value.isVideoReady).isTrue()
    }

    @Test
    fun `GIVEN nothing rendered yet WHEN playback fails THEN shutter lifts to the still frame`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)

        // Act
        model.onError()

        // Assert
        assertThat(model.uiState.value.isVideoReady).isTrue()
    }

    @Test
    fun `GIVEN surface already gone WHEN playback fails THEN shutter stays down`() {
        // Arrange
        val model = createModel()
        surfaceAttached(isAttached = true)
        model.attachSurface(surfaceView)
        model.onFirstFrameRendered()
        surfaceAttached(isAttached = false)
        model.detachSurface(surfaceView)

        // Act
        model.onError()

        // Assert
        assertThat(model.uiState.value.isVideoReady).isFalse()
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
    fun `GIVEN introduction screen WHEN legal links clicked THEN each opens its own document`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onTermsOfServiceClick()
        model.uiState.value.onPrivacyPolicyClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/terms-of-service/") }
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/privacy-policy/") }
    }

    @Test
    fun `GIVEN introduction screen WHEN wallet buttons clicked THEN nothing happens yet`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onCreateWalletClick()
        model.uiState.value.onIHaveWalletClick()

        // Assert
        verify { urlOpener wasNot Called }
        assertThat(model.uiState.value.renderedFields()).isEqualTo(SHUTTER_DOWN)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LaunchMode {

        @ParameterizedTest
        @ProvideTestModels
        fun launchMode(model: LaunchModeModel) {
            // Act
            val introductionModel = createModel(launchMode = model.launchMode)

            // A canary meant to fail once [REDACTED_TASK_KEY] makes an NFC launch start the scan.
            assertThat(introductionModel.uiState.value.renderedFields()).isEqualTo(SHUTTER_DOWN)
            assertThat(sentEvents.map { it::class }).containsExactly(IntroductionProcess.ScreenOpened::class)
        }

        private fun provideTestModels() = listOf(
            LaunchModeModel(InitScreenLaunchMode.Standard),
            LaunchModeModel(InitScreenLaunchMode.WithCardScan),
        )
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

    internal data class LaunchModeModel(val launchMode: InitScreenLaunchMode)

    internal data class RunningModel(val isRunning: Boolean)

    private fun IntroductionUM.renderedFields() = isVideoReady to isMotionEnabled

    private fun surfaceAttached(isAttached: Boolean) {
        every { videoPlayer.isSurfaceAttached } returns isAttached
    }

    private fun createModel(launchMode: InitScreenLaunchMode = InitScreenLaunchMode.Standard) = IntroductionModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(IntroductionComponent.Params(launchMode)),
        videoPlayerFactory = videoPlayerFactory,
        urlOpener = urlOpener,
        analyticsEventHandler = analyticsEventHandler,
    )

    private companion object {
        val SHUTTER_DOWN = false to true
    }
}