package com.tangem.features.introduction.impl.model

import android.view.SurfaceView
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
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
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) : Model(), IntroductionVideoPlayer.Listener, HaveWalletSheetController.Host {

    private val params = paramsContainer.require<IntroductionComponent.Params>()

    val storiesNavigation = SlotNavigation<Unit>()

    private var isScreenResumed = false
    private var isStoryShown = false

    /** The scan tap being served; cleared once settled or once the viewer closes the story around it. */
    private var pendingScanStoryAction: StoriesV2PendingAction? = null

    /** An ended story kept on screen because navigation is covering it; dismissed on the next resume. */
    private var isStoryDismissPendingResume = false
    private var isCreatingMobileWallet = false
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

    val uiState: StateFlow<IntroductionUM>
        field = MutableStateFlow(
            IntroductionUM(
                isVideoReady = false,
                isMotionEnabled = videoPlayer.isMotionEnabled,
                bottomSheetConfig = null,
                haveWalletBottomSheetConfig = null,
                onCreateWalletClick = ::onCreateWalletClick,
                onIHaveWalletClick = ::onIHaveWalletClick,
                onTermsOfServiceClick = ::onTermsOfServiceClick,
                onPrivacyPolicyClick = ::onPrivacyPolicyClick,
            ),
        )

    override val haveWalletSheetConfig: TangemBottomSheetConfig? get() = uiState.value.haveWalletBottomSheetConfig

    init {
        reportScreenOpened()
        if (params.launchMode == InitScreenLaunchMode.WithCardScan) {
            launchCardScan(onNavigatedAway = {}, onFailed = {})
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
        if (!videoPlayer.isSurfaceAttached || uiState.value.isVideoReady) return
        uiState.update { it.copy(isVideoReady = true) }
    }

    override fun onError() {
        val stage = if (hasRenderedFrame) {
            IntroductionProcess.WelcomeMediaError.Stage.Playback
        } else {
            IntroductionProcess.WelcomeMediaError.Stage.Load
        }
        analyticsEventHandler.send(IntroductionProcess.WelcomeMediaError(stage))
        // Errors arrive asynchronously, so one can land after the surface is gone; lifting the shutter then
        // would uncover a blank surface on the way back.
        if (!videoPlayer.isSurfaceAttached) return
        uiState.update { it.copy(isVideoReady = true) }
    }

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
        if (isRunning && isStoryDismissPendingResume) {
            // The viewer backed out of the flow that covered the ended story: welcome, without the sheet.
            isStoryDismissPendingResume = false
            isStoryShown = false
            storiesNavigation.dismiss()
        }
        applyVideoRunning()
    }

    /**
     * Two independent reasons to stop the background video, so neither may override the other: the screen is not
     * resumed, or the story is on top of it. A slot child leaves its host resumed, so returning from background
     * while the story plays would otherwise restart the video behind it.
     */
    private fun applyVideoRunning() {
        videoPlayer.setRunning(isRunning = isScreenResumed && !isStoryShown)
    }

    private fun onCreateWalletClick() {
        if (isAnySheetShown()) return
        analyticsEventHandler.send(IntroductionProcess.ButtonCreateNewWallet())
        reportCreateWalletSheetOpened()
        uiState.update { it.copy(bottomSheetConfig = createWalletBottomSheetConfig()) }
    }

    private fun reportCreateWalletSheetOpened() {
        modelScope.launch {
            val referralId = referral.readId()
            analyticsEventHandler.send(
                IntroductionProcess.CreateWalletIntroScreenOpened(screenType = null, referralId = referralId),
            )
        }
    }

    // TODO [REDACTED_TASK_KEY]: the icons of both sheets are placeholders until the design hands over the assets.
    private fun createWalletBottomSheetConfig() = TangemBottomSheetConfig(
        isShown = true,
        onDismissRequest = ::onCreateWalletSheetDismissRequested,
        content = CreateWalletUM(
            cardOrRing = WalletOptionUM(
                iconRes = CoreUiR.drawable.ic_tangem_24,
                title = resourceReference(CoreResR.string.introduction_create_wallet_hardware_title),
                description = resourceReference(CoreResR.string.introduction_create_wallet_hardware_description),
                isEnabled = !isCreatingMobileWallet,
                onClick = ::onTangemCardOrRingClick,
            ),
            seedPhrase = WalletOptionUM(
                iconRes = CoreUiR.drawable.ic_seed_phrase_24,
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

    private fun onTangemCardOrRingClick() {
        if (!isCreateWalletSheetShown() || isCreatingMobileWallet) return
        analyticsEventHandler.send(IntroductionProcess.ButtonHardwareWallet())
        onBottomSheetDismissed()
        isStoryShown = true
        applyVideoRunning()
        storiesNavigation.activate(Unit)
    }

    /**
     * The story hands back every terminal decision instead of navigating, so this owns the routing table for it.
     * Kept short on purpose: for a bundled composition the result can arrive re-entrantly, inside `activate`.
     */
    fun onStoryResult(result: StoriesV2Result) {
        when (result) {
            // A looping story reports neither on its own; both mean the viewer left it alone. Unavailable draws
            // no frame at all, so as far as the viewer can tell they never left the sheet.
            StoriesV2Result.Dismissed,
            StoriesV2Result.Completed,
            is StoriesV2Result.Unavailable,
            -> {
                pendingScanStoryAction = null
                closeStory()
                reopenCreateWalletSheet()
            }
            // A scan that navigated away is covered by the next screen: tearing the player down now would flash
            // the welcome screen behind the closing NFC sheet, so that teardown waits for the next resume.
            is StoriesV2Result.ActionInvoked -> when (result.actionId) {
                SCAN_WALLET_ACTION_ID -> isStoryDismissPendingResume = true
                else -> closeStory()
            }
        }
    }

    /**
     * Keyed on the action id rather than the target type: the id is what the composition promises, while several
     * actions can share a target type. A remotely delivered action this build does not know must land the viewer
     * back where they acted from instead of on a bare screen.
     */
    fun onStoryAction(action: StoriesV2PendingAction) {
        when (action.actionId) {
            LEARN_MORE_ACTION_ID -> openTangemStore(action)
            SCAN_WALLET_ACTION_ID -> startCardScan(action)
            else -> {
                TangemLogger.e("Unhandled story action: id=${action.actionId}, target=${action.target}")
                action.resume()
            }
        }
    }

    private fun closeStory() {
        isStoryShown = false
        applyVideoRunning()
        storiesNavigation.dismiss()
    }

    private fun openTangemStore(action: StoriesV2PendingAction) {
        analyticsEventHandler.send(Basic.ButtonBuy(source = AnalyticsParam.ScreensSources.Stories))
        // Deliberately a terminal exit: the tap ends the story rather than suspending it around the browser.
        action.end()
        modelScope.launch {
            // The url the story carries is the bare storefront; the generated one adds the language, the utm
            // campaign and the referral promocode, which is what every other buy entry point reports on.
            urlOpener.openUrl(generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Creation))
        }
    }

    /**
     * The screen is torn down while the story is up, so the modal's own remembered state is gone and the flag is
     * enough to replay its open animation; the config still holds the callbacks bound to this model.
     */
    private fun reopenCreateWalletSheet() {
        reportCreateWalletSheetOpened()
        uiState.update { state ->
            val restored = state.bottomSheetConfig?.copy(isShown = true) ?: createWalletBottomSheetConfig()
            state.copy(bottomSheetConfig = restored)
        }
    }

    private fun startCardScan(action: StoriesV2PendingAction) {
        pendingScanStoryAction = action
        analyticsEventHandler.send(IntroductionProcess.ButtonScanCard(AnalyticsParam.ScreensSources.Intro))
        launchCardScan(onNavigatedAway = ::onScanNavigatedAway, onFailed = ::onScanFailed)
    }

    private fun launchCardScan(onNavigatedAway: () -> Unit, onFailed: () -> Unit) {
        modelScope.launch {
            cardScanner.scanAndOpenWallet(
                analyticsSource = AnalyticsParam.ScreensSources.Intro,
                onNavigatedAway = onNavigatedAway,
                onFailed = onFailed,
            )
        }
    }

    private fun onScanNavigatedAway() {
        // The closing analytics must beat the navigation that kills the tree.
        pendingScanStoryAction?.end()
        pendingScanStoryAction = null
    }

    private fun onScanFailed() {
        val action = pendingScanStoryAction
        pendingScanStoryAction = null
        // Without a story to resume, the viewer closed it while the scan ran: the sheet is where they act from now.
        if (action != null) action.resume() else reopenCreateWalletSheet()
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
        if (isAnySheetShown() || isCreatingMobileWallet) return
        analyticsEventHandler.send(IntroductionProcess.ButtonIHaveWallet())
        haveWalletSheet.open()
    }

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
        launchCardScan(onNavigatedAway = {}, onFailed = {})
    }

    override fun ensureHotWalletCreationSupported(): Boolean {
        if (isHotWalletCreationSupported()) return true
        uiMessageSender.send(hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()))
        return false
    }

    private fun onTermsOfServiceClick() {
        openLegalDocument(IntroductionProcess.LegalOpened.Document.TermsOfService, TERMS_OF_SERVICE_URL)
    }

    private fun onPrivacyPolicyClick() {
        openLegalDocument(IntroductionProcess.LegalOpened.Document.PrivacyPolicy, PRIVACY_POLICY_URL)
    }

    private fun openLegalDocument(document: IntroductionProcess.LegalOpened.Document, url: String) {
        analyticsEventHandler.send(IntroductionProcess.LegalOpened(document))
        urlOpener.openUrl(url)
    }

    private companion object {
        // Declared by the bundled composition in BundledStoryV2ContentSource; a remote one must reuse them.
        const val LEARN_MORE_ACTION_ID = "learn_more"
        const val SCAN_WALLET_ACTION_ID = "scan_wallet"
        const val TERMS_OF_SERVICE_URL = "https://tangem.com/terms-of-service/"
        const val PRIVACY_POLICY_URL = "https://tangem.com/privacy-policy/"
    }
}