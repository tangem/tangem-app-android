package com.tangem.features.tangempay.ui

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
import com.tangem.features.tangempay.TangemPayMainBannerState

@Composable
internal fun TangemPayMainScreenBlock(
    state: TangemPayMainBannerState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TangemPayMainBannerState.Progress -> TangemPayProgressState(state, modifier)
        is TangemPayMainBannerState.Card -> TangemPayCardMainBlock(state, isBalanceHidden, modifier)
        is TangemPayMainBannerState.Empty -> Unit
        is TangemPayMainBannerState.RefreshNeeded -> TangemPayRefreshBlock(state, modifier)
        is TangemPayMainBannerState.TemporaryUnavailable -> TangemPayUnavailableBlock(state, modifier)
        is TangemPayMainBannerState.FailedIssue -> TangemPayFailedIssueState(state, modifier)
        is TangemPayMainBannerState.OnboardingBanner -> TangemPayOnboardingBanner(state, modifier)
        is TangemPayMainBannerState.ExposedDevice -> TangemPayExposedDeviceState(modifier)
        is TangemPayMainBannerState.Loading -> TangemPayLoadingScreenBlock(modifier)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayMainScreenBlockPreview() {
    TangemThemePreview {
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
            TangemPayMainScreenBlock(state = TangemPayMainBannerState.Loading, isBalanceHidden = false)

            TangemPayMainScreenBlock(state = TangemPayMainBannerState.ExposedDevice, isBalanceHidden = false)

            TangemPayMainScreenBlock(
                TangemPayMainBannerState.Progress(
                    title = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_title),
                    description = TextReference.EMPTY,
                    buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
                    iconRes = R.drawable.ic_promo_kyc_36,
                    onButtonClick = {},
                ),
                isBalanceHidden = false,
            )

            TangemPayMainScreenBlock(
                TangemPayMainBannerState.Progress(
                    title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
                    description = TextReference.EMPTY,
                    buttonText = TextReference.Res(R.string.common_continue),
                    iconRes = R.drawable.ic_tangem_pay_promo_card_36,
                    onButtonClick = {},
                ),
                isBalanceHidden = false,
            )

            TangemPayMainScreenBlock(
                TangemPayMainBannerState.Progress(
                    title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
                    description = TextReference.Res(R.string.tangempay_issue_card_notification_description),
                    buttonText = TextReference.EMPTY,
                    iconRes = R.drawable.ic_tangem_pay_promo_card_36,
                    onButtonClick = {},
                    shouldShowProgress = true,
                ),
                isBalanceHidden = false,
            )

            TangemPayMainScreenBlock(
                TangemPayMainBannerState.Card(
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