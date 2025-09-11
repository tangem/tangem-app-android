package com.tangem.features.onboarding.v2.multiwallet.impl.child.upgradewallet.ui.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.onboarding.v2.common.ui.OnboardingDialogUM

internal data class MultiWalletUpgradeWalletUM(
    val title: TextReference,
    val bodyText: TextReference,
    val onStartUpgradeClick: () -> Unit,
    val dialog: OnboardingDialogUM?,
)