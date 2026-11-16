package com.tangem.features.introduction.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.cloudbackup.usecase.ResolveCloudRestoreBackupsUseCase
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import kotlinx.coroutines.CoroutineScope
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList")
internal class HaveWalletSheetController(
    private val host: Host,
    scope: CoroutineScope,
    router: Router,
    analyticsEventHandler: AnalyticsEventHandler,
    hotWalletFeatureToggles: HotWalletFeatureToggles,
    resolveCloudRestoreBackups: ResolveCloudRestoreBackupsUseCase,
    private val socialLoginProvider: () -> SocialLoginProvider?,
    private val isSigningInSocially: () -> Boolean,
    private val onSocialLoginClick: (SocialLoginProvider) -> Unit,
) : WalletOptionsSheetController(
    host = host,
    scope = scope,
    router = router,
    analyticsEventHandler = analyticsEventHandler,
    hotWalletFeatureToggles = hotWalletFeatureToggles,
    resolveCloudRestoreBackups = resolveCloudRestoreBackups,
    isLocked = isSigningInSocially,
) {

    interface Host : WalletOptionsSheetController.Host {

        fun scanCardForExistingWallet()
    }

    override val optionsSheet = IntroductionProcess.SheetDismissed.Sheet.IHaveWallet

    fun open() {
        analyticsEventHandler.send(IntroductionProcess.IHaveWalletIntroScreenOpened())
        showOptions()
    }

    // Every row is gated, not just its own: the others would start a scan on top of a flow about to navigate away.
    override fun optionsStep() = HaveWalletUM.Step.Options(
        cardOrRing = WalletOptionUM(
            icon = WalletOptionUM.Icon.Tinted(Icons.ic_logo_tangem_20),
            title = resourceReference(CoreResR.string.introduction_create_wallet_hardware_title),
            description = resourceReference(CoreResR.string.introduction_i_have_wallet_hardware_description),
            isEnabled = !isSigningInSocially(),
            onClick = ::onSignInWithCardClick,
        ),
        socialLogin = socialLoginProvider()?.let { provider ->
            WalletOptionUM(
                icon = WalletOptionUM.Icon.Image(CoreUiR.drawable.img_google_32),
                title = resourceReference(
                    id = CoreResR.string.introduction_i_have_wallet_social_title,
                    formatArgs = wrappedList(provider.displayName),
                ),
                description = resourceReference(
                    id = CoreResR.string.introduction_i_have_wallet_social_description,
                    formatArgs = wrappedList(provider.displayName),
                ),
                isEnabled = !isSigningInSocially(),
                isLoading = isSigningInSocially(),
                isLoaderSpaceReserved = true,
                onClick = { onSocialLoginOptionClick(provider) },
            )
        },
        importWallet = importWalletOption(onClick = ::onImportWalletClick),
    )

    /**
     * Closed before the scan and not reopened after it: scan failures arrive as a global snackbar, which would draw
     * under the sheet's dialog window.
     */
    private fun onSignInWithCardClick() {
        if (!isShown || isSigningInSocially()) return
        analyticsEventHandler.send(IntroductionProcess.ButtonSignInWithCard())
        dismiss()
        host.scanCardForExistingWallet()
    }

    /** The sheet stays up: the row spins here until the flow has a screen of its own to show */
    private fun onSocialLoginOptionClick(provider: SocialLoginProvider) {
        if (!isShown || isSigningInSocially()) return
        onSocialLoginClick(provider)
    }

    private fun onImportWalletClick() {
        if (!isShown || isSigningInSocially() || isOnImportStep) return
        analyticsEventHandler.send(IntroductionProcess.ButtonImportExistingWallet())
        openImportStep()
    }
}