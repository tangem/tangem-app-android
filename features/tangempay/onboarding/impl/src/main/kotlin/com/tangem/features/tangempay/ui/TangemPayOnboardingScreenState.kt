package com.tangem.features.tangempay.ui

import javax.annotation.concurrent.Immutable

@Immutable
internal data class TangemPayOnboardingScreenState(
    val fullScreenLoading: Boolean = true,
    val buttonLoading: Boolean = false,
)