package com.tangem.features.introduction.impl.model

import android.view.SurfaceView
import androidx.compose.ui.unit.IntSize
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletCreationNotSupportedDialog
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.core.ui.res.generated.icons.ic_wallet_plus_20
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.cloudbackup.usecase.ResolveCloudRestoreBackupsUseCase
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.R
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.features.introduction.impl.ui.state.CreateWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.features.storiesv2.StoriesV2PendingAction
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import com.tangem.core.res.R as CoreResR
import kotlinx.coroutines.withContext

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class IntroductionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    videoPlayerFactory: IntroductionVideoPlayer.Factory,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val entryReasonTracker: IntroductionEntryReasonTracker,
    private val router: Router,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val resolveCloudRestoreBackups: ResolveCloudRestoreBackupsUseCase,
    private val trackingContextProxy: TrackingContextProxy,
    private val cardScanner: IntroductionCardScanner,
    private val mobileWalletCreator: IntroductionMobileWalletCreator,
    private val referral: IntroductionReferral,
    private val legalLinks: IntroductionLegalLinks,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) : Model(), IntroductionVideoPlayer.Listener, HaveWalletSheetController.Host, IntroductionStories.Host {

    private val params = paramsContainer.require<IntroductionComponent.Params>()

    private var isScreenResumed = false
    private var isVideoRestartPendingResume = false
    private var isCreatingMobileWallet = false
    private var isScanningCard = false
    private var hasRenderedFrame = false

    private val videoPlayer = videoPlayerFactory.create(
        videoRes = R.raw.introduction_background,
        listener = this,
    )

    private val haveWalletSheet = HaveWalletSheetController(
        host = this,
        scope = modelScope,
        router = router,
        analyticsEventHandler = analyticsEventHandler,
        socialLoginFeatureToggles = socialLoginFeatureToggles,
        hotWalletFeatureToggles = hotWalletFeatureToggles,
        resolveCloudRestoreBackups = resolveCloudRestoreBackups,
    )

    private val stories = IntroductionStories(
        host = this,
        scope = modelScope,
        analyticsEventHandler = analyticsEventHandler,
        urlOpener = urlOpener,
        generateBuyTangemCardLinkUseCase = generateBuyTangemCardLinkUseCase,
    )

    private val store = IntroductionStore(
        scope = modelScope,
        analyticsEventHandler = analyticsEventHandler,
        urlOpener = urlOpener,
        generateBuyTangemCardLinkUseCase = generateBuyTangemCardLinkUseCase,
    )

    val storiesNavigation: SlotNavigation<Unit> get() = stories.navigation

    private val createWalletSheetCollapsed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val uiState: StateFlow<IntroductionUM>
        field = MutableStateFlow(
            IntroductionUM(
                isVideoReady = false,
                videoSize = BACKGROUND_VIDEO_SIZE,
                videoSurfaceKey = 0,
                isVideoFailed = false,
                isVideoRunning = false,
                isMotionEnabled = videoPlayer.isMotionEnabled,
                scenes = welcomeScenes,
                bottomSheetConfig = null,
                haveWalletBottomSheetConfig = null,
                onCreateWalletClick = ::onCreateWalletClick,
                onIHaveWalletClick = ::onIHaveWalletClick,
                onBuyTangemClick = ::onBuyTangemClick,
                onCreateWalletSheetCollapsed = ::onCreateWalletSheetCollapsed,
                onTermsOfServiceClick = legalLinks::openTermsOfService,
            ),
        )

    override val haveWalletSheetConfig: TangemBottomSheetConfig? get() = uiState.value.haveWalletBottomSheetConfig

    private val outputWatchdog = IntroductionVideoOutputWatchdog(
        scope = modelScope,
        videoPlayer = videoPlayer,
        onReplaceSurface = ::replaceVideoSurface,
        onGiveUp = ::onError,
    )

    init {
        reportScreenOpened()
        if (params.launchMode == InitScreenLaunchMode.WithCardScan) {
            launchCardScan(onNavigatedAway = ::onTerminalFlowEntered, onFailed = {})
        }
    }

    override fun onDestroy() {
        videoPlayer.release()
        super.onDestroy()
    }

    /** Not cancellable: an NFC launch can open the wallet before the reason resolves, and the opening still counts. */
    private fun reportScreenOpened() {
        modelScope.launch { withContext(NonCancellable) { entryReasonTracker.reportOpening(params.entryReason) } }
    }

    override fun onFirstFrameRendered() {
        hasRenderedFrame = true
        updateVideoReady()
    }

    /**
     * The surface keeps [BACKGROUND_VIDEO_SIZE] whatever the file turns out to be: a new surface would race the first
     * frame the old one reports right after this, and the cropping scale mode fits a file that disagrees into it.
     */
    override fun onVideoSizeChanged(width: Int, height: Int) {
        if (IntSize(width, height) == BACKGROUND_VIDEO_SIZE) return
        TangemLogger.e("Welcome video is ${width}x$height, but $BACKGROUND_VIDEO_SIZE is declared for it")
    }

    override fun onPlayingChanged(isPlaying: Boolean) = outputWatchdog.onPlayingChanged(isPlaying)

    /** The new surface is composed before the old one is released, and stays empty until its first frame. */
    private fun replaceVideoSurface() {
        uiState.update { it.copy(videoSurfaceKey = it.videoSurfaceKey + 1, isVideoReady = false) }
    }

    private fun updateVideoReady() {
        if (!hasRenderedFrame || !videoPlayer.isSurfaceAttached) return
        uiState.update { it.copy(isVideoReady = true) }
    }

    override fun onError(error: Throwable) {
        val stage = if (hasRenderedFrame) {
            IntroductionProcess.WelcomeMediaError.Stage.Playback
        } else {
            IntroductionProcess.WelcomeMediaError.Stage.Load
        }
        analyticsEventHandler.send(IntroductionProcess.WelcomeMediaError(stage))
        TangemLogger.e("Welcome background video failed at the ${stage.value} stage", error)
        // Only a rendered frame lifts the shutter, and the player never recovers from an error: uncovering the
        // SurfaceView here would expose the hole it punches through the window (see IntroductionScreen).
        uiState.update { it.copy(isVideoFailed = true) }
        // A player error stops the player by itself; a stalled output given up on by the watchdog does not.
        applyVideoRunning()
    }

    fun videoPositionMs(): Long = videoPlayer.positionMs

    fun attachSurface(surfaceView: SurfaceView) {
        videoPlayer.attachSurface(surfaceView)
    }

    fun detachSurface(surfaceView: SurfaceView) {
        videoPlayer.detachSurface(surfaceView)
        if (videoPlayer.isSurfaceAttached) return
        uiState.update { it.copy(isVideoReady = false) }
    }

    fun setVideoRunning(isRunning: Boolean) {
        isScreenResumed = isRunning
        if (isRunning) {
            stories.onScreenResumed()
            store.onScreenResumed()
        }
        if (isRunning && isVideoRestartPendingResume) {
            isVideoRestartPendingResume = false
            videoPlayer.restart()
        }
        applyVideoRunning()
    }

    override fun onStoryVisibilityChanged() = applyVideoRunning()

    /**
     * Independent reasons to stop the background video, so none may override another: the screen is not resumed, the
     * story is on top of it, or the video has failed. A slot child leaves its host resumed, so returning from
     * background while the story plays would otherwise restart the video behind it.
     */
    private fun applyVideoRunning() {
        val isRunning = isScreenResumed && !stories.isShown && !uiState.value.isVideoFailed
        videoPlayer.setRunning(isRunning = isRunning)
        uiState.update { it.copy(isVideoRunning = isRunning) }
    }

    private fun onCreateWalletClick() {
        if (isAnySheetShown() || stories.isOpening) return
        analyticsEventHandler.send(IntroductionProcess.ButtonCreateNewWallet())
        modelScope.launch {
            val referralId = referral.readId()
            analyticsEventHandler.send(
                IntroductionProcess.CreateWalletIntroScreenOpened(screenType = null, referralId = referralId),
            )
        }
        uiState.update { it.copy(bottomSheetConfig = createWalletBottomSheetConfig()) }
    }

    private fun createWalletBottomSheetConfig() = TangemBottomSheetConfig(
        isShown = true,
        onDismissRequest = ::onCreateWalletSheetDismissRequested,
        content = CreateWalletUM(
            cardOrRing = WalletOptionUM(
                icon = Icons.ic_logo_tangem_20,
                title = resourceReference(CoreResR.string.introduction_create_wallet_hardware_title),
                description = resourceReference(CoreResR.string.introduction_create_wallet_hardware_description),
                isEnabled = !isCreatingMobileWallet,
                onClick = ::onTangemCardOrRingClick,
            ),
            seedPhrase = WalletOptionUM(
                icon = Icons.ic_wallet_plus_20,
                title = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_title),
                description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
                isLoading = isCreatingMobileWallet,
                onClick = ::onCreateSeedPhraseWalletClick,
            ),
            onCloseClick = ::onCreateWalletSheetCloseClicked,
        ),
    )

    private fun onCreateWalletSheetDismissRequested() {
        reportCreateWalletSheetDismissed(IntroductionProcess.SheetDismissed.DismissReason.Dismissed)
    }

    private fun onCreateWalletSheetCloseClicked() {
        reportCreateWalletSheetDismissed(IntroductionProcess.SheetDismissed.DismissReason.CloseButton)
    }

    private fun reportCreateWalletSheetDismissed(reason: IntroductionProcess.SheetDismissed.DismissReason) {
        if (!isCreateWalletSheetShown()) return
        analyticsEventHandler.send(
            IntroductionProcess.SheetDismissed(
                sheet = IntroductionProcess.SheetDismissed.Sheet.CreateWallet,
                reason = reason,
            ),
        )
        onBottomSheetDismissed()
    }

    /** Keeps the content so the sheet can play its exit animation instead of vanishing. */
    private fun onBottomSheetDismissed() {
        uiState.update { it.copy(bottomSheetConfig = it.bottomSheetConfig?.copy(isShown = false)) }
    }

    private fun onCreateWalletSheetCollapsed() {
        stories.onSheetCollapsed()
        createWalletSheetCollapsed.tryEmit(Unit)
    }

    /**
     * The sheet is an overlay of the host window, so it outlives the composition of the screen that opened it and
     * would hang over the wallet for the length of its collapse. The stack swaps once it is gone.
     */
    private suspend fun collapseCreateWalletSheet() {
        if (!isCreateWalletSheetShown()) return
        onBottomSheetDismissed()
        withTimeoutOrNull(SHEET_COLLAPSE_TIMEOUT) { createWalletSheetCollapsed.first() }
    }

    private fun onTangemCardOrRingClick() {
        if (!isCreateWalletSheetShown() || isCreatingMobileWallet) return
        analyticsEventHandler.send(IntroductionProcess.ButtonHardwareWallet())
        onBottomSheetDismissed()
        stories.openAfterSheetCollapse()
    }

    fun onStoryResult(result: StoriesV2Result) = stories.onResult(result)

    fun onStoryAction(action: StoriesV2PendingAction) = stories.onAction(action)

    override fun scanCardFromStory() {
        analyticsEventHandler.send(IntroductionProcess.ButtonScanCard(AnalyticsParam.ScreensSources.Intro))
        launchCardScan(onNavigatedAway = ::onScanNavigatedAway, onFailed = stories::onScanFailed)
    }

    private fun launchCardScan(onNavigatedAway: () -> Unit, onFailed: () -> Unit) {
        modelScope.launch {
            isScanningCard = true
            try {
                cardScanner.scanAndOpenWallet(
                    analyticsSource = AnalyticsParam.ScreensSources.Intro,
                    onNavigatedAway = onNavigatedAway,
                    onFailed = onFailed,
                )
            } finally {
                isScanningCard = false
            }
        }
    }

    private fun onScanNavigatedAway() {
        stories.onScanNavigatedAway()
        onTerminalFlowEntered()
    }

    private fun onCreateSeedPhraseWalletClick() {
        if (!isCreateWalletSheetShown() || isCreatingMobileWallet) return
        // Released only if no wallet gets saved: a saved wallet installs its own context in place of this one.
        trackingContextProxy.addHotWalletContext()
        analyticsEventHandler.send(
            OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet(
                source = AnalyticsParam.ScreensSources.CreateWalletIntro.value,
            ),
        )
        if (!ensureHotWalletCreationSupported()) {
            onBottomSheetDismissed()
            trackingContextProxy.removeContext()
            return
        }
        setMobileWalletCreating(isCreating = true)
        modelScope.launch {
            mobileWalletCreator.createAndOpen(
                source = AnalyticsParam.ScreensSources.CreateWalletIntro,
                beforeOpen = ::collapseCreateWalletSheet,
                onFailed = {
                    trackingContextProxy.removeContext()
                    setMobileWalletCreating(isCreating = false)
                },
            )
        }
    }

    private fun setMobileWalletCreating(isCreating: Boolean) {
        isCreatingMobileWallet = isCreating
        uiState.update { state ->
            val config = state.bottomSheetConfig?.let { createWalletBottomSheetConfig().copy(isShown = it.isShown) }
            state.copy(bottomSheetConfig = config)
        }
    }

    private fun onIHaveWalletClick() {
        if (isAnySheetShown() || isCreatingMobileWallet || stories.isOpening) return
        analyticsEventHandler.send(IntroductionProcess.ButtonIHaveWallet())
        haveWalletSheet.open()
    }

    private fun onBuyTangemClick() {
        if (isAnySheetShown() || isWalletFlowRunning() || isStoryOnScreen()) return
        store.open()
    }

    private fun isWalletFlowRunning(): Boolean = isCreatingMobileWallet || isScanningCard

    private fun isStoryOnScreen(): Boolean = stories.isOpening || stories.isShown

    private fun isAnySheetShown(): Boolean = isCreateWalletSheetShown() || isHaveWalletSheetShown()

    /**
     * A dismissed sheet keeps its dialog composed and interactive until the collapse ends, so a tap landing on a
     * row after the close button still reaches it. Every row that acts checks the flag first.
     */
    private fun isCreateWalletSheetShown(): Boolean = uiState.value.bottomSheetConfig?.isShown == true

    private fun isHaveWalletSheetShown(): Boolean = haveWalletSheet.isShown

    override fun updateHaveWalletSheetConfig(transform: (TangemBottomSheetConfig?) -> TangemBottomSheetConfig?) {
        uiState.update { it.copy(haveWalletBottomSheetConfig = transform(it.haveWalletBottomSheetConfig)) }
    }

    /** The sheet row reports itself as `Button - Sign In With Card`; `Button - Scan Card` belongs to the story. */
    override fun scanCardForExistingWallet() {
        launchCardScan(onNavigatedAway = ::onTerminalFlowEntered, onFailed = {})
    }

    override fun onTerminalFlowEntered() {
        isVideoRestartPendingResume = true
    }

    override fun ensureHotWalletCreationSupported(): Boolean {
        if (isHotWalletCreationSupported()) return true
        uiMessageSender.send(hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()))
        return false
    }

    private companion object {
        val SHEET_COLLAPSE_TIMEOUT = 600.milliseconds

        /**
         * The size of `R.raw.introduction_background`, declared rather than read from the file so the surface takes
         * its final shape before the first frame: resizing it under a playing decoder is what can get its output stuck.
         */
        val BACKGROUND_VIDEO_SIZE = IntSize(width = 1080, height = 1920)
    }
}