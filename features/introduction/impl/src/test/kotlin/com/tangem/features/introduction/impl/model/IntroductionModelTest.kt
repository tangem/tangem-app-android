package com.tangem.features.introduction.impl.model

import android.view.SurfaceView
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
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.features.introduction.impl.ui.state.CreateWalletUM
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.res.R as CoreResR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionModelTest {

    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val videoPlayer: IntroductionVideoPlayer = mockk(relaxed = true)
    private val videoPlayerFactory: IntroductionVideoPlayer.Factory = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk()
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxed = true)
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
            trackingContextProxy,
            uiMessageSender,
        )
        every { isHotWalletCreationSupported() } returns true
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
                .containsExactly(IntroductionProcess.ScreenOpened(), IntroductionProcess.ButtonCreateNewWallet())
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
            model.uiState.value.bottomSheetConfig?.onDismissRequest?.invoke()

            // Assert
            val config = model.uiState.value.bottomSheetConfig
            assertThat(config?.isShown).isFalse()
            assertThat(config?.content).isInstanceOf(CreateWalletUM::class.java)
        }

        @Test
        fun `GIVEN a supported device WHEN the seed phrase option is picked THEN the creation flow opens`() {
            // Arrange
            val model = createModel()

            // Act
            model.openSheet().seedPhrase.onClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
            verify(exactly = 1) { trackingContextProxy.addHotWalletContext() }
            // The creation flow owns the context from here on and releases it on its own destroy.
            verify(exactly = 0) { trackingContextProxy.removeContext() }
            verify(exactly = 1) {
                router.push(AppRoute.CreateMobileWallet(AnalyticsParam.ScreensSources.CreateWalletIntro))
            }
            assertThat(sentEvents.last()).isEqualTo(
                OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet(
                    source = AnalyticsParam.ScreensSources.CreateWalletIntro.value,
                ),
            )
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
            verify { router wasNot Called }
            // Nothing downstream opens, so the context added for the tap has to be given back here.
            verifyOrder {
                trackingContextProxy.addHotWalletContext()
                trackingContextProxy.removeContext()
            }
        }

        @Test
        fun `GIVEN an open sheet WHEN the card option is picked THEN it closes and leads nowhere yet`() {
            // Arrange
            val model = createModel()

            // Act
            model.openSheet().cardOrRing.onClick()

            // A canary meant to fail once [REDACTED_TASK_KEY] wires the Onboarding Stories to this option.
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isFalse()
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN a dismissed sheet WHEN create wallet clicked again THEN it opens anew`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onCreateWalletClick()
            model.uiState.value.bottomSheetConfig?.onDismissRequest?.invoke()

            // Act
            model.uiState.value.onCreateWalletClick()

            // Assert
            assertThat(model.uiState.value.bottomSheetConfig?.isShown).isTrue()
            assertThat(sentEvents.filterIsInstance<IntroductionProcess.ButtonCreateNewWallet>()).hasSize(2)
        }

        private fun IntroductionModel.openSheet(): CreateWalletUM {
            uiState.value.onCreateWalletClick()
            return uiState.value.bottomSheetConfig?.content as CreateWalletUM
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HaveWalletSheet {

        @Test
        fun `GIVEN no sheet WHEN i have a wallet clicked THEN the options open and the tap is reported`() {
            // Arrange
            val model = createModel()

            // Act
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            // A canary meant to fail once the analytics contract for this tap is settled.
            assertThat(sentEvents).containsExactly(IntroductionProcess.ScreenOpened())
        }

        @Test
        fun `GIVEN an open sheet WHEN i have a wallet clicked again THEN it stays the same sheet`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onIHaveWalletClick()
            val opened = model.uiState.value.haveWalletBottomSheetConfig

            // Act
            model.uiState.value.onIHaveWalletClick()

            // Assert
            assertThat(model.uiState.value.haveWalletBottomSheetConfig).isSameInstanceAs(opened)
        }

        @Test
        fun `GIVEN the options step WHEN import picked THEN the import step replaces it and nothing is reported`() {
            // Arrange
            val model = createModel()

            // Act
            model.openHaveWallet().importWallet.onClick()

            // Assert
            assertThat(model.haveWalletStep()).isInstanceOf(HaveWalletUM.Step.Import::class.java)
            assertThat(model.uiState.value.haveWalletBottomSheetConfig?.isShown).isTrue()
            // Picking a path inside a sheet must stay silent; only the flow it opens reports itself.
            assertThat(sentEvents).containsExactly(IntroductionProcess.ScreenOpened())
        }

        @Test
        fun `GIVEN the import step WHEN back pressed THEN the options return and the sheet stays open`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()

            // Act
            (model.haveWalletStep() as HaveWalletUM.Step.Import).onBack()

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
            model.uiState.value.haveWalletBottomSheetConfig?.onDismissRequest?.invoke()

            // Assert
            val config = model.uiState.value.haveWalletBottomSheetConfig
            assertThat(config?.isShown).isFalse()
            assertThat(config?.content).isInstanceOf(HaveWalletUM::class.java)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun inertRows(model: InertRowModel) {
            // Arrange
            val introductionModel = createModel()
            val options = introductionModel.openHaveWallet()

            // Act
            model.pick(options, introductionModel)

            // A canary meant to fail once [REDACTED_TASK_KEY] wires these rows to their flows.
            verify { router wasNot Called }
        }

        private fun provideTestModels() = listOf(
            InertRowModel(name = "card or ring") { options, _ -> options.cardOrRing.onClick() },
            InertRowModel(name = "sign in") { options, _ -> options.signIn.onClick() },
            InertRowModel(name = "recovery phrase") { options, model ->
                options.importWallet.onClick()
                (model.haveWalletStep() as HaveWalletUM.Step.Import).onRecoveryPhraseClick()
            },
            InertRowModel(name = "cloud backup") { options, model ->
                options.importWallet.onClick()
                (model.haveWalletStep() as HaveWalletUM.Step.Import).onCloudBackupClick()
            },
        )

        @Test
        fun `GIVEN the sheet dismissed on the import step WHEN reopened THEN it starts from the options again`() {
            // Arrange
            val model = createModel()
            model.openHaveWallet().importWallet.onClick()
            model.uiState.value.haveWalletBottomSheetConfig?.onDismissRequest?.invoke()

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
            model.uiState.value.haveWalletBottomSheetConfig?.onDismissRequest?.invoke()

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
            assertThat(options.signIn.title)
                .isEqualTo(resourceReference(CoreResR.string.introduction_i_have_wallet_sign_in_google_title))
            assertThat(options.importWallet.title)
                .isEqualTo(resourceReference(CoreResR.string.hw_import_existing_wallet))
        }

        private fun IntroductionModel.openHaveWallet(): HaveWalletUM.Step.Options {
            uiState.value.onIHaveWalletClick()
            return haveWalletStep() as HaveWalletUM.Step.Options
        }
    }

    internal data class InertRowModel(
        val name: String,
        val pick: (HaveWalletUM.Step.Options, IntroductionModel) -> Unit,
    ) {
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

    private fun IntroductionModel.haveWalletStep(): HaveWalletUM.Step? =
        (uiState.value.haveWalletBottomSheetConfig?.content as? HaveWalletUM)?.step

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
        router = router,
        isHotWalletCreationSupported = isHotWalletCreationSupported,
        trackingContextProxy = trackingContextProxy,
        uiMessageSender = uiMessageSender,
    )

    private companion object {
        val SHUTTER_DOWN = false to true
        const val LEAST_OS_VERSION = "Android 10"


    }
}