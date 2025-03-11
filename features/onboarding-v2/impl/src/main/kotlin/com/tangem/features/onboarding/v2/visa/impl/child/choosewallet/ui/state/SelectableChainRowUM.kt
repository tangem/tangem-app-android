package com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui.state

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.OnboardingVisaChooseWalletComponent

internal data class SelectableChainRowUM(
    val event: OnboardingVisaChooseWalletComponent.Params.Event,
    @DrawableRes val icon: Int,
    val text: TextReference,
)