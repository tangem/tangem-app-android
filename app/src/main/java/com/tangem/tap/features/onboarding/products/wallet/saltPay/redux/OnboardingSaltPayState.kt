package com.tangem.tap.features.onboarding.products.wallet.saltPay.redux

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.SaltPayWorkaround
import com.tangem.tap.common.toggleWidget.WidgetState
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayActivationManager
import com.tangem.tap.features.wallet.redux.ProgressState
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
data class OnboardingSaltPayState(
    @Transient
    val saltPayManager: SaltPayActivationManager = SaltPayActivationManager.stub(),
    val pinCode: String? = null,
    val accessCode: String? = null,
    val amountToClaim: Amount? = null,
    val tokenAmount: Amount = Amount(SaltPayWorkaround.tokenFrom(Blockchain.SaltPay), BigDecimal.ZERO),
    val step: SaltPayActivationStep = SaltPayActivationStep.None,
    val saltPayCardArtworkUrl: String? = null,
    val inProgress: Boolean = false,
    val claimInProgress: Boolean = false,
) {

    val mainButtonState: WidgetState
        get() = if (inProgress) ProgressState.Loading else ProgressState.Done

    val pinLength: Int = 4
}

enum class SaltPayActivationStep {
    None,
    NoGas,
    NeedPin,
    CardRegistration,
    KycIntro,
    KycStart,
    KycWaiting,
    KycReject,
    Claim,
    ClaimInProgress,
    ClaimSuccess,
    Success,
    Finished;
}