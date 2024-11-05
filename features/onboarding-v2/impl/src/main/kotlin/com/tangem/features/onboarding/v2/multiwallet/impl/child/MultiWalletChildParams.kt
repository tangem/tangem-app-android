package com.tangem.features.onboarding.v2.multiwallet.impl.child

import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.flow.MutableStateFlow

class MultiWalletChildParams(
    val multiWalletState: MutableStateFlow<OnboardingMultiWalletState>,
    val parentParams: OnboardingMultiWalletComponent.Params,
)
