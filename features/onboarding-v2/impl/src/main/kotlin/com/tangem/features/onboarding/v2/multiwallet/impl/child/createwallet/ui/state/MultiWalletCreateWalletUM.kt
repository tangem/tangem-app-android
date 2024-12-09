package com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.ui.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.OnboardingDialogUM

internal data class MultiWalletCreateWalletUM(
    val title: TextReference,
    val bodyText: TextReference,
    val showOtherOptionsButton: Boolean,
    val onCreateWalletClick: () -> Unit,
    val onOtherOptionsClick: () -> Unit,
    val dialog: OnboardingDialogUM?,
)