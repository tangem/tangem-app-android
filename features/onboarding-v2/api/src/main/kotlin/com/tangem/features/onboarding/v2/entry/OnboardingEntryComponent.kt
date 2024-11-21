package com.tangem.features.onboarding.v2.entry

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse

interface OnboardingEntryComponent : ComposableContentComponent {

    data class Params(
        val scanResponse: ScanResponse,
        val startBackupFlow: Boolean,
    )

    interface Factory : ComponentFactory<Params, OnboardingEntryComponent>
}
