package com.tangem.features.introduction.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.core.ui.res.generated.icons.ic_user_20
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.cloudbackup.usecase.ResolveCloudRestoreBackupsUseCase
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.cloudrestore.CloudRestoreErrorMessage
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.tangem.core.res.R as CoreResR

@Suppress("LongParameterList")
internal class HaveWalletSheetController(
    private val host: Host,
    private val scope: CoroutineScope,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val resolveCloudRestoreBackups: ResolveCloudRestoreBackupsUseCase,
) {

    interface Host {

        val haveWalletSheetConfig: TangemBottomSheetConfig?

        fun updateHaveWalletSheetConfig(transform: (TangemBottomSheetConfig?) -> TangemBottomSheetConfig?)

        fun scanCardForExistingWallet()

        fun onTerminalFlowEntered()

        /** Shows the unsupported-device warning when it returns `false`. */
        fun ensureHotWalletCreationSupported(): Boolean
    }

    private var cloudResolveJob: Job? = null

    val isShown: Boolean get() = host.haveWalletSheetConfig?.isShown == true

    fun open() {
        analyticsEventHandler.send(IntroductionProcess.IHaveWalletIntroScreenOpened())
        showStep(::optionsStep)
    }

    private fun optionsStep() = HaveWalletUM.Step.Options(
        cardOrRing = WalletOptionUM(
            icon = Icons.ic_logo_tangem_20,
            title = resourceReference(CoreResR.string.introduction_create_wallet_hardware_title),
            description = resourceReference(CoreResR.string.introduction_i_have_wallet_hardware_description),
            onClick = ::onSignInWithCardClick,
        ),
        socialLogin = if (socialLoginFeatureToggles.isSocialLoginEnabled) {
            WalletOptionUM(
                icon = Icons.ic_user_20,
                title = resourceReference(CoreResR.string.introduction_i_have_wallet_sign_in_google_title),
                description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
                // TODO TWI-1555: social login belongs to another team; the row stays inert until it lands.
                onClick = {},
            )
        } else {
            null
        },
        importWallet = WalletOptionUM(
            icon = Icons.ic_arrow_down_20,
            title = resourceReference(CoreResR.string.hw_import_existing_wallet),
            // The sheet's own wording: the hw_ description belongs to the mobile-wallet screens and reads
            // differently there.
            description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
            onClick = ::onImportWalletClick,
        ),
    )

    /**
     * Closed before the scan and not reopened after it: scan failures arrive as a global snackbar, which would draw
     * under the sheet's dialog window.
     */
    private fun onSignInWithCardClick() {
        if (!isShown) return
        analyticsEventHandler.send(IntroductionProcess.ButtonSignInWithCard())
        dismiss()
        host.scanCardForExistingWallet()
    }

    private fun onImportWalletClick() {
        if (!isShown) return
        if (currentStep() is HaveWalletUM.Step.Import) return
        analyticsEventHandler.send(IntroductionProcess.ButtonImportExistingWallet())
        analyticsEventHandler.send(
            OnboardingAnalyticsEvent.Backup.ImportWalletRequest(cloudBackup = cloudBackupAvailability()),
        )
        showStep { importStep(isCloudLoading = false) }
    }

    private fun cloudBackupAvailability() = if (hotWalletFeatureToggles.isGoogleDriveBackupEnabled) {
        AnalyticsParam.CloudBackupAvailability.Available
    } else {
        AnalyticsParam.CloudBackupAvailability.Unavailable
    }

    private fun currentStep(): HaveWalletUM.Step? = (host.haveWalletSheetConfig?.content as? HaveWalletUM)?.step

    private fun importStep(isCloudLoading: Boolean) = HaveWalletUM.Step.Import(
        title = resourceReference(CoreResR.string.hw_import_existing_wallet),
        description = resourceReference(CoreResR.string.hw_import_existing_wallet_description),
        recoveryPhraseButtonText = resourceReference(CoreResR.string.hw_cloud_backup_restore_use_recovery_phrase),
        cloudBackupButtonText = if (hotWalletFeatureToggles.isGoogleDriveBackupEnabled) {
            resourceReference(
                id = CoreResR.string.hw_import_restore_cloud_backup,
                formatArgs = wrappedList(resourceReference(CoreResR.string.hw_cloud_backup_service_name)),
            )
        } else {
            null
        },
        isCloudLoading = isCloudLoading,
        onRecoveryPhraseClick = ::onRecoveryPhraseClick,
        onCloudBackupClick = ::onCloudBackupClick,
        onBack = ::onImportBack,
    )

    private fun onImportBack() {
        if (importStepOrNull() == null) return
        analyticsEventHandler.send(
            IntroductionProcess.SheetDismissed(
                sheet = IntroductionProcess.SheetDismissed.Sheet.ImportExisting,
                reason = IntroductionProcess.SheetDismissed.DismissReason.BackButton,
            ),
        )
        cancelCloudResolve()
        showStep(::optionsStep)
    }

    private fun onRecoveryPhraseClick() {
        if (importStepOrNull()?.isCloudLoading != false) return
        dismiss()
        if (!host.ensureHotWalletCreationSupported()) return
        openImport(AppRoute.AddExistingWallet.Mode.RecoveryPhrase)
    }

    /**
     * The device check comes before the account picker: after it the backups would already be parked for a restore
     * screen that never opens.
     */
    private fun onCloudBackupClick() {
        if (importStepOrNull()?.isCloudLoading != false || !hotWalletFeatureToggles.isGoogleDriveBackupEnabled) return
        if (!host.ensureHotWalletCreationSupported()) {
            dismiss()
            return
        }
        showStep { importStep(isCloudLoading = true) }
        cloudResolveJob = scope.launch {
            val result = resolveCloudRestoreBackups()
            if (!isShown) {
                cloudResolveJob = null
                return@launch
            }
            result.fold(
                ifLeft = { error ->
                    val message = CloudRestoreErrorMessage.of(error)
                    if (message == null) showStep { importStep(isCloudLoading = false) } else showError(message)
                },
                ifRight = {
                    cloudResolveJob = null
                    dismiss()
                    openImport(AppRoute.AddExistingWallet.Mode.CloudRestore)
                },
            )
            cloudResolveJob = null
        }
    }

    /** Unlike the mobile wallet screen, the sheet does not report the onboarding start, so the import flow does. */
    private fun openImport(mode: AppRoute.AddExistingWallet.Mode) {
        host.onTerminalFlowEntered()
        router.push(AppRoute.AddExistingWallet(mode = mode, shouldSendOnboardingStarted = true))
    }

    /** The step on screen, if it is Import: the outgoing step stays tappable while the next one slides in. */
    private fun importStepOrNull(): HaveWalletUM.Step.Import? =
        (currentStep() as? HaveWalletUM.Step.Import)?.takeIf { isShown }

    private fun cancelCloudResolve() {
        cloudResolveJob?.cancel()
        cloudResolveJob = null
    }

    private fun showError(message: CloudRestoreErrorMessage) {
        showStep { HaveWalletUM.Step.Error(message = message, onGotItClick = ::onCloseClick) }
    }

    private fun showStep(step: () -> HaveWalletUM.Step) {
        val content = HaveWalletUM(step = step(), onCloseClick = ::onCloseClick)
        host.updateHaveWalletSheetConfig { current ->
            current?.copy(isShown = true, content = content)
                ?: TangemBottomSheetConfig(isShown = true, onDismissRequest = ::onDismissRequest, content = content)
        }
    }

    private fun onCloseClick() = dismissByViewer(IntroductionProcess.SheetDismissed.DismissReason.CloseButton)

    private fun onDismissRequest() = dismissByViewer(IntroductionProcess.SheetDismissed.DismissReason.Dismissed)

    /** Only a viewer leaving without picking a row is a dismissal; rows dismiss programmatically and stay silent. */
    private fun dismissByViewer(reason: IntroductionProcess.SheetDismissed.DismissReason) {
        if (!isShown) return
        val sheet = when (currentStep()) {
            is HaveWalletUM.Step.Options -> IntroductionProcess.SheetDismissed.Sheet.IHaveWallet
            is HaveWalletUM.Step.Import,
            is HaveWalletUM.Step.Error,
            null,
            -> IntroductionProcess.SheetDismissed.Sheet.ImportExisting
        }
        analyticsEventHandler.send(IntroductionProcess.SheetDismissed(sheet = sheet, reason = reason))
        dismiss()
    }

    private fun dismiss() {
        cancelCloudResolve()
        host.updateHaveWalletSheetConfig { it?.copy(isShown = false) }
    }
}