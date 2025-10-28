package com.tangem.features.tangempay.ui

import javax.annotation.concurrent.Immutable

@Immutable
internal data class TangemPayOnboardingScreenState(
    val fullScreenLoading: Boolean,
    val buttonLoading: Boolean,
    val onGetCardClick: () -> Unit,
    val onBackClick: () -> Unit,
)