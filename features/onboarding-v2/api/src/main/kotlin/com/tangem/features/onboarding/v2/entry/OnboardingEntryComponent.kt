package com.tangem.features.onboarding.v2.entry

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWalletId

interface OnboardingEntryComponent : ComposableContentComponent {

    data class Params(
        val scanResponse: ScanResponse,
        val mode: Mode,
    )

    sealed class Mode {
        data object Onboarding : Mode()
        data object AddBackupWallet1 : Mode()
        data object WelcomeOnlyTwin : Mode()
        data object RecreateWalletTwin : Mode()
        data object ContinueFinalize : Mode()
        data class UpgradeHotWallet(val userWalletId: UserWalletId) : Mode()
    }

    interface Factory : ComponentFactory<Params, OnboardingEntryComponent>
}