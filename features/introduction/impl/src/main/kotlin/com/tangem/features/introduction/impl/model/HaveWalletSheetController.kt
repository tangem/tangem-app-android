package com.tangem.features.introduction.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletCreationNotSupportedDialog
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

/**
 * The «I have a wallet» sheet: its two steps and where every row leads. The sheet's config lives in the screen
 * state, which the [Host] owns; this class only decides what goes into it.
 */
internal class HaveWalletSheetController(
    private val host: Host,
    private val router: Router,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val trackingContextProxy: TrackingContextProxy,
    private val uiMessageSender: UiMessageSender,
) {

    interface Host {

        val haveWalletSheetConfig: TangemBottomSheetConfig?

        fun updateHaveWalletSheetConfig(transform: (TangemBottomSheetConfig?) -> TangemBottomSheetConfig?)

        /** Starts the scan for a viewer who already owns a Tangem wallet; the sheet is closed by then. */
        fun scanCardForExistingWallet()
    }

    /**
     * The import flow runs under the hot-wallet analytics context, like it does when entered from the mobile
     * wallet screen — only there the screen's own model removes the context on destroy, while here the flow is
     * pushed straight over Introduction, so the context is dropped when the viewer comes back.
     */
    private var isHotWalletContextPendingResume = false

    /**
     * A dismissed sheet keeps its dialog composed and interactive until the collapse ends, so a tap landing on a
     * row after the close button still reaches it. Every row that acts checks the flag first.
     */
    val isShown: Boolean get() = host.haveWalletSheetConfig?.isShown == true

    fun open() = showStep(::optionsStep)

    fun onScreenResumed() {
        if (!isHotWalletContextPendingResume) return
        isHotWalletContextPendingResume = false
        trackingContextProxy.removeContext()
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

    /**
     * The sheet closes before the scan and stays closed whatever the scan ends in: its failures arrive through the
     * global message sender, whose snackbar would draw under the sheet's dialog window, and a cancelled scan
     * leaves the viewer on the welcome screen — the same as on iOS.
     */
    private fun onSignInWithCardClick() {
        if (!isShown) return
        dismiss()
        host.scanCardForExistingWallet()
    }

    // Picking a path inside a sheet is deliberately not reported; the import flow reports itself once it opens.
    private fun onImportWalletClick() {
        if (!isShown) return
        if (currentStep() is HaveWalletUM.Step.Import) return
        showStep(::importStep)
    }

    private fun currentStep(): HaveWalletUM.Step? = (host.haveWalletSheetConfig?.content as? HaveWalletUM)?.step

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
        onBack = { showStep(::optionsStep) },
    )

    private fun onRecoveryPhraseClick() {
        if (!isShown) return
        dismiss()
        if (!isHotWalletCreationSupported()) {
            uiMessageSender.send(
                hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()),
            )
            return
        }
        trackingContextProxy.addHotWalletContext()
        isHotWalletContextPendingResume = true
        router.push(AppRoute.AddExistingWallet(mode = AppRoute.AddExistingWallet.Mode.RecoveryPhrase))
    }

    private fun onCloudBackupClick() {
        // TODO [REDACTED_TASK_KEY]: AppRoute.AddExistingWallet(Mode.CloudRestore) only works once the discovered backups
        //  have been put into CloudRestoreResultHolder — arriving there with an empty holder silently pops back.
    }

    private fun showStep(step: () -> HaveWalletUM.Step) {
        val content = HaveWalletUM(step = step())
        host.updateHaveWalletSheetConfig { current ->
            current?.copy(isShown = true, content = content)
                ?: TangemBottomSheetConfig(isShown = true, onDismissRequest = ::dismiss, content = content)
        }
    }

    private fun dismiss() {
        host.updateHaveWalletSheetConfig { it?.copy(isShown = false) }
    }
}