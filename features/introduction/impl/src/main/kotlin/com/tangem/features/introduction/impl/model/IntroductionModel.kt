package com.tangem.features.introduction.impl.model

import android.view.SurfaceView
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
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
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletCreationNotSupportedDialog
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.R
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.features.introduction.impl.ui.state.CreateWalletUM
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.features.storiesv2.StoriesV2PendingAction
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList")
@ModelScoped
internal class IntroductionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    videoPlayerFactory: IntroductionVideoPlayer.Factory,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val trackingContextProxy: TrackingContextProxy,
    private val cardScanner: IntroductionCardScanner,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) : Model(), IntroductionVideoPlayer.Listener {

    private val params = paramsContainer.require<IntroductionComponent.Params>()

    val storiesNavigation = SlotNavigation<Unit>()

    private var isScreenResumed = false
    private var isStoryShown = false

    /** The scan tap being served; cleared once settled or once the viewer closes the story around it. */
    private var pendingScanStoryAction: StoriesV2PendingAction? = null

    /** An ended story kept on screen because navigation is covering it; dismissed on the next resume. */
    private var isStoryDismissPendingResume = false

    private val videoPlayer = videoPlayerFactory.create(
        videoRes = R.raw.introduction_background,
        listener = this,
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

    init {
        analyticsEventHandler.send(IntroductionProcess.ScreenOpened())

        when (params.launchMode) {
            // TODO [REDACTED_TASK_KEY]: start the card scan here and drop the WithCardScan exception in ChildFactory;
            //  it needs the progress and error states the screen has no design for yet.
            InitScreenLaunchMode.WithCardScan,
            InitScreenLaunchMode.Standard,
            -> Unit
        }
    }

    override fun onDestroy() {
        videoPlayer.release()
        super.onDestroy()
    }

    override fun onFirstFrameRendered() {
        if (!videoPlayer.isSurfaceAttached || uiState.value.isVideoReady) return
        uiState.update { it.copy(isVideoReady = true) }
    }

    override fun onError() {
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
        // TODO [REDACTED_TASK_KEY]: send "Create Wallet Intro Screen Opened" here on every open — it needs the referral
        //  id and a Screen Type the analyst has to pick, since this sheet offers both wallet types.
        uiState.update { it.copy(bottomSheetConfig = createWalletBottomSheetConfig()) }
    }

    // TODO [REDACTED_TASK_KEY]: the icons of both sheets are placeholders until the design hands over the assets.
    private fun createWalletBottomSheetConfig() = TangemBottomSheetConfig(
        isShown = true,
        onDismissRequest = ::onBottomSheetDismissed,
        content = CreateWalletUM(
            cardOrRing = WalletOptionUM(
                iconRes = CoreUiR.drawable.ic_tangem_24,
                title = resourceReference(CoreResR.string.introduction_create_wallet_hardware_title),
                description = resourceReference(CoreResR.string.introduction_create_wallet_hardware_description),
                onClick = ::onTangemCardOrRingClick,
            ),
            seedPhrase = WalletOptionUM(
                iconRes = CoreUiR.drawable.ic_seed_phrase_24,
                title = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_title),
                description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
                onClick = ::onCreateSeedPhraseWalletClick,
            ),
        ),
    )

    /** Keeps the content so the sheet can play its exit animation instead of vanishing. */
    private fun onBottomSheetDismissed() {
        uiState.update { it.copy(bottomSheetConfig = it.bottomSheetConfig?.copy(isShown = false)) }
    }

    private fun onTangemCardOrRingClick() {
        if (!isCreateWalletSheetShown()) return
        // TODO [REDACTED_TASK_KEY]: report this tap once the analytics contract is settled. The mobile twin has
        //  ButtonMobileWallet, but no event exists for picking the hardware option: CreateWalletSelectionModel
        //  sends none either, and Settings.ButtonAddHardwareWallet is another category and needs a wallet.
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
        uiState.update { state ->
            state.copy(
                bottomSheetConfig = state.bottomSheetConfig?.copy(isShown = true)
                    ?: createWalletBottomSheetConfig(),
            )
        }
    }

    private fun startCardScan(action: StoriesV2PendingAction) {
        pendingScanStoryAction = action
        val analyticsSource = AnalyticsParam.ScreensSources.Intro
        analyticsEventHandler.send(IntroductionProcess.ButtonScanCard(analyticsSource))
        modelScope.launch {
            cardScanner.scanAndOpenWallet(
                analyticsSource = analyticsSource,
                onNavigatedAway = ::onScanNavigatedAway,
                onFailed = ::onScanFailed,
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
        if (action != null) {
            action.resume()
        } else {
            // The viewer closed the story while the scan ran; the sheet is where they act from now.
            reopenCreateWalletSheet()
        }
    }

    private fun onCreateSeedPhraseWalletClick() {
        if (!isCreateWalletSheetShown()) return
        onBottomSheetDismissed()
        trackingContextProxy.addHotWalletContext()
        analyticsEventHandler.send(
            OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet(
                source = AnalyticsParam.ScreensSources.CreateWalletIntro.value,
            ),
        )
        if (!isHotWalletCreationSupported()) {
            trackingContextProxy.removeContext()
            uiMessageSender.send(
                hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()),
            )
            return
        }
        // TODO [REDACTED_TASK_KEY]: returning from this terminal flow has to replay the video and the scene text from
        //  their first frame; both belong to the Welcome screen task, which owns the player and the text layer.
        router.push(AppRoute.CreateMobileWallet(AnalyticsParam.ScreensSources.CreateWalletIntro))
    }

    private fun onIHaveWalletClick() {
        if (isAnySheetShown()) return
        // TODO [REDACTED_TASK_KEY]: report this tap once the analytics contract is settled. The spec asks for
        //  "[Sign In] Button - Open Existing Account" and calls it an existing event, but the only event of
        //  that name lives in the "Settings / Wallet Settings" category and is about accounts, and the
        //  properties it is meant to keep are unknown.
        showHaveWalletStep(::optionsStep)
    }

    private fun isAnySheetShown(): Boolean = isCreateWalletSheetShown() || isHaveWalletSheetShown()

    /**
     * A dismissed sheet keeps its dialog composed and interactive until the collapse ends, so a tap landing on a
     * row after the close button still reaches it. Every row that acts checks the flag first.
     */
    private fun isCreateWalletSheetShown(): Boolean = uiState.value.bottomSheetConfig?.isShown == true

    private fun isHaveWalletSheetShown(): Boolean = uiState.value.haveWalletBottomSheetConfig?.isShown == true

    private fun optionsStep() = HaveWalletUM.Step.Options(
        cardOrRing = WalletOptionUM(
            iconRes = CoreUiR.drawable.ic_tangem_card_24,
            title = resourceReference(CoreResR.string.introduction_create_wallet_hardware_title),
            description = resourceReference(CoreResR.string.introduction_i_have_wallet_hardware_description),
            onClick = ::onSignInWithCardClick,
        ),
        signIn = WalletOptionUM(
            iconRes = CoreUiR.drawable.ic_user_24,
            title = resourceReference(CoreResR.string.introduction_i_have_wallet_sign_in_google_title),
            description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
            // TODO TWI-1555: social login belongs to another team; the row stays inert until it lands.
            onClick = {},
        ),
        importWallet = WalletOptionUM(
            iconRes = CoreUiR.drawable.ic_arrow_down_24,
            title = resourceReference(CoreResR.string.hw_import_existing_wallet),
            description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
            onClick = ::onImportWalletClick,
        ),
    )

    private fun onSignInWithCardClick() {
        // TODO [REDACTED_TASK_KEY]: start the card scan here, once it has the states noted at the launch-mode branch.
    }

    // Picking a path inside a sheet is deliberately not reported; the import flow reports itself once it opens.
    private fun onImportWalletClick() {
        if (!isHaveWalletSheetShown()) return
        if (currentHaveWalletStep() is HaveWalletUM.Step.Import) return
        showHaveWalletStep(::importStep)
    }

    private fun currentHaveWalletStep(): HaveWalletUM.Step? =
        (uiState.value.haveWalletBottomSheetConfig?.content as? HaveWalletUM)?.step

    private fun importStep() = HaveWalletUM.Step.Import(
        title = resourceReference(CoreResR.string.hw_import_existing_wallet),
        description = resourceReference(CoreResR.string.hw_import_existing_wallet_description),
        recoveryPhraseButtonText = resourceReference(CoreResR.string.hw_cloud_backup_restore_use_recovery_phrase),
        cloudBackupButtonText = resourceReference(
            id = CoreResR.string.hw_import_restore_cloud_backup,
            formatArgs = wrappedList(resourceReference(CoreResR.string.hw_cloud_backup_service_name)),
        ),
        onRecoveryPhraseClick = ::onRecoveryPhraseClick,
        onCloudBackupClick = ::onCloudBackupClick,
        onBack = { showHaveWalletStep(::optionsStep) },
    )

    private fun onRecoveryPhraseClick() {
        // TODO [REDACTED_TASK_KEY]: open AppRoute.AddExistingWallet(Mode.RecoveryPhrase) once navigation lands.
    }

    private fun onCloudBackupClick() {
        // TODO [REDACTED_TASK_KEY]: the cloud button belongs behind HotWalletFeatureToggles.isGoogleDriveBackupEnabled,
        //  and AppRoute.AddExistingWallet(Mode.CloudRestore) only works once the discovered backups have been
        //  put into CloudRestoreResultHolder — arriving there with an empty holder silently pops straight back.
    }

    private fun showHaveWalletStep(step: () -> HaveWalletUM.Step) {
        val content = HaveWalletUM(step = step())
        uiState.update { state ->
            val current = state.haveWalletBottomSheetConfig
            state.copy(
                haveWalletBottomSheetConfig = current?.copy(isShown = true, content = content)
                    ?: TangemBottomSheetConfig(
                        isShown = true,
                        onDismissRequest = ::dismissHaveWalletSheet,
                        content = content,
                    ),
            )
        }
    }

    private fun dismissHaveWalletSheet() {
        uiState.update {
            it.copy(haveWalletBottomSheetConfig = it.haveWalletBottomSheetConfig?.copy(isShown = false))
        }
    }

    private fun onTermsOfServiceClick() {
        urlOpener.openUrl(TERMS_OF_SERVICE_URL)
    }

    private fun onPrivacyPolicyClick() {
        urlOpener.openUrl(PRIVACY_POLICY_URL)
    }

    private companion object {
        // Declared by the bundled composition in BundledStoryV2ContentSource; a remote one must reuse them.
        const val LEARN_MORE_ACTION_ID = "learn_more"
        const val SCAN_WALLET_ACTION_ID = "scan_wallet"
        const val TERMS_OF_SERVICE_URL = "https://tangem.com/terms-of-service/"
        const val PRIVACY_POLICY_URL = "https://tangem.com/privacy-policy/"
    }
}