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
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

internal class HaveWalletSheetController(
    private val host: Host,
    private val router: Router,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles,
    private val trackingContextProxy: TrackingContextProxy,
    private val uiMessageSender: UiMessageSender,
) {

    interface Host {

        val haveWalletSheetConfig: TangemBottomSheetConfig?

        fun updateHaveWalletSheetConfig(transform: (TangemBottomSheetConfig?) -> TangemBottomSheetConfig?)

        fun scanCardForExistingWallet()
    }

    /**
     * Entered from the mobile wallet screen, the import flow gets its hot-wallet analytics context removed by that
     * screen's destroy; pushed straight over Introduction it has no such owner, so the context goes on resume.
     */
    private var isHotWalletContextPendingResume = false

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
        socialLogin = if (socialLoginFeatureToggles.isSocialLoginEnabled) {
            WalletOptionUM(
                iconRes = CoreUiR.drawable.ic_user_24,
                title = resourceReference(CoreResR.string.introduction_i_have_wallet_sign_in_google_title),
                description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
                // TODO TWI-1555: social login belongs to another team; the row stays inert until it lands.
                onClick = {},
            )
        } else {
            null
        },
        importWallet = WalletOptionUM(
            iconRes = CoreUiR.drawable.ic_arrow_down_24,
            title = resourceReference(CoreResR.string.hw_import_existing_wallet),
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
        dismiss()
        host.scanCardForExistingWallet()
    }

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
        // TODO: wired by the cloud backup team; AddExistingWallet(Mode.CloudRestore) needs the backups resolved first.
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