package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState.Progress
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.TangemPayCardMainBlock

@Composable
internal fun TangemPayMainScreenBlock(state: TangemPayState, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is Progress -> TangemPayProgressState(state)
        is TangemPayState.Card -> TangemPayCardMainBlock(state, isBalanceHidden, modifier)
        is TangemPayState.Empty -> Unit
        is TangemPayState.RefreshNeeded -> TangemPayRefreshBlock(state, modifier)
        is TangemPayState.TemporaryUnavailable -> TangemPayUnavailableBlock(state, modifier)
        is TangemPayState.FailedIssue -> TangemPayFailedIssueState(state)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayMainScreenBlockPreview() {
    TangemThemePreview {
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
            TangemPayMainScreenBlock(
                Progress(
                    title = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_title),
                    description = TextReference.EMPTY,
                    buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
                    iconRes = R.drawable.ic_promo_kyc_36,
                    onButtonClick = {},
                ),
                isBalanceHidden = false,
            )

            TangemPayMainScreenBlock(
                Progress(
                    title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
                    description = TextReference.EMPTY,
                    buttonText = TextReference.Res(R.string.common_continue),
                    iconRes = R.drawable.ic_tangem_pay_promo_card_36,
                    onButtonClick = {},
                ),
                isBalanceHidden = false,
            )

            TangemPayMainScreenBlock(
                Progress(
                    title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
                    description = TextReference.Res(R.string.tangempay_issue_card_notification_description),
                    buttonText = TextReference.EMPTY,
                    iconRes = R.drawable.ic_tangem_pay_promo_card_36,
                    onButtonClick = {},
                    showProgress = true,
                ),
                isBalanceHidden = false,
            )

            TangemPayMainScreenBlock(
                TangemPayState.Card(
                    lastFourDigits = TextReference.Str("*1234"),
                    balanceText = TextReference.Str("$ 0.00"),
                    balanceSymbol = TextReference.Str("USDC"),
                    onClick = {},
                ),
                isBalanceHidden = false,
            )
        }
    }
}