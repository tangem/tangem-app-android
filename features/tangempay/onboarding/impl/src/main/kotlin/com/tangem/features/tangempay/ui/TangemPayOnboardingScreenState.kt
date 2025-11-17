package com.tangem.features.tangempay.ui

import javax.annotation.concurrent.Immutable

@Immutable
internal sealed class TangemPayOnboardingScreenState {
    abstract val onBack: () -> Unit

    data class Loading(override val onBack: () -> Unit) : TangemPayOnboardingScreenState()
    data class Content(
        override val onBack: () -> Unit,
        val onTermsClick: () -> Unit,
        val buttonConfig: ButtonConfig,
    ) : TangemPayOnboardingScreenState() {
        data class ButtonConfig(val isLoading: Boolean, val onClick: () -> Unit)
    }
}