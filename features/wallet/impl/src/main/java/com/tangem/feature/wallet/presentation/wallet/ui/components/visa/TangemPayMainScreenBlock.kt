package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState.Progress
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.TangemPayCardMainBlock

@Composable
internal fun TangemPayMainScreenBlock(state: TangemPayState, modifier: Modifier = Modifier) {
    when (state) {
        is Progress -> {
            Notification(
                modifier = modifier
                    .fillMaxWidth(),
                config = NotificationConfig(
                    title = state.title,
                    iconSize = 36.dp,
                    subtitle = TextReference.EMPTY,
                    iconResId = state.iconRes,
                    buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                        text = state.buttonText,
                        onClick = state.onButtonClick,
                        shouldShowProgress = state.showProgress,
                    ),
                ),
            )
        }
        is TangemPayState.Card -> TangemPayCardMainBlock(state, modifier)
        is TangemPayState.Empty -> Unit
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResetCardScreenPreview() {
    TangemThemePreview {
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
            TangemPayMainScreenBlock(
                Progress(
                    title = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_title),
                    buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
                    iconRes = R.drawable.ic_promo_kyc_36,
                    onButtonClick = {},
                ),
            )

            TangemPayMainScreenBlock(
                Progress(
                    title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
                    buttonText = TextReference.Res(R.string.common_continue),
                    iconRes = R.drawable.ic_tangem_pay_promo_card_36,
                    onButtonClick = {},
                ),
            )

            TangemPayMainScreenBlock(
                Progress(
                    title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
                    buttonText = TextReference.EMPTY,
                    iconRes = R.drawable.ic_tangem_pay_promo_card_36,
                    onButtonClick = {},
                    showProgress = true,
                ),
            )

            TangemPayMainScreenBlock(
                TangemPayState.Card(
                    lastFourDigits = TextReference.Str("*1234"),
                    balanceText = TextReference.Str("$ 0.00"),
                    onClick = {},
                ),
            )
        }
    }
}