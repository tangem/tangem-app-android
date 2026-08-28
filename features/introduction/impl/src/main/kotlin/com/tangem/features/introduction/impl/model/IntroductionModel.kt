package com.tangem.features.introduction.impl.model

import android.view.SurfaceView
import com.tangem.common.routing.AppRoute
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
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletCreationNotSupportedDialog
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.R
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.features.introduction.impl.ui.state.CreateWalletUM
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) : Model(), IntroductionVideoPlayer.Listener {

    private val params = paramsContainer.require<IntroductionComponent.Params>()

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
        videoPlayer.setRunning(isRunning)
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
        onBottomSheetDismissed()
        // TODO [REDACTED_TASK_KEY]: open the Onboarding Stories. Here the hardware option sells the wallet rather than
        //  scanning it, unlike the same-named option of the I have a wallet sheet.
    }

    private fun onCreateSeedPhraseWalletClick() {
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

    private fun isAnySheetShown(): Boolean = uiState.value.run {
        bottomSheetConfig?.isShown == true || haveWalletBottomSheetConfig?.isShown == true
    }

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
        const val TERMS_OF_SERVICE_URL = "https://tangem.com/terms-of-service/"
        const val PRIVACY_POLICY_URL = "https://tangem.com/privacy-policy/"
    }
}