package com.tangem.feature.wallet.presentation.wallet.state.util

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState.Progress

internal object TangemPayStateCreator {

    fun createKycInProgressState(onClickKyc: () -> Unit): TangemPayState = Progress(
        title = TextReference.Res(R.string.tangempay_payment_account),
        description = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_title),
        buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
        iconRes = R.drawable.ic_promo_kyc_36,
        onButtonClick = onClickKyc,
    )

    fun createIssueProgressState(onIssuingCardClick: () -> Unit): TangemPayState = Progress(
        title = TextReference.Res(R.string.tangempay_payment_account),
        description = TextReference.Res(R.string.tangempay_issuing_your_card),
        buttonText = TextReference.EMPTY,
        iconRes = R.drawable.ic_tangem_pay_promo_card_36,
        onButtonClick = onIssuingCardClick,
        showProgress = true,
    )

    fun createCancelledState(onIssueFailedClick: () -> Unit): TangemPayState = TangemPayState.FailedIssue(
        title = TextReference.Res(R.string.tangempay_payment_account),
        description = TextReference.Res(R.string.tangempay_failed_to_issue_card),
        iconRes = R.drawable.ic_alert_24,
        onButtonClick = onIssueFailedClick,
    )
}