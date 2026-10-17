package com.tangem.features.tangempay.addfunds.bankTransferOnramp.onboarding

import androidx.compose.runtime.Immutable
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM

@Immutable
internal data class TangemPayBankTransferOnrampOnboardingScreenUM(
    val fees: TangemPayOnrampFeesUM,
    val isLoading: Boolean,
    val onContinueClick: () -> Unit,
    val onTermsClick: () -> Unit,
    val onCloseClick: () -> Unit,
)