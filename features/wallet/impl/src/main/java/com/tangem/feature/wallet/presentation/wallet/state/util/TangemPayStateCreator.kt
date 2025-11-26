package com.tangem.feature.wallet.presentation.wallet.state.util

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState.Progress

internal object TangemPayStateCreator {

    fun createKycInProgressState(onClickKyc: () -> Unit): TangemPayState = Progress(
        title = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_title),
        description = TextReference.EMPTY,
        buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
        iconRes = R.drawable.ic_promo_kyc_36,
        onButtonClick = onClickKyc,
    )

    fun createIssueProgressState(): TangemPayState = Progress(
        title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
        description = TextReference.Res(R.string.tangempay_issue_card_notification_description),
        buttonText = TextReference.EMPTY,
        iconRes = R.drawable.ic_tangem_pay_promo_card_36,
        onButtonClick = {},
        showProgress = true,
    )
}