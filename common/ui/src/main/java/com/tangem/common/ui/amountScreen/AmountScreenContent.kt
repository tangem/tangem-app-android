package com.tangem.common.ui.amountScreen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.preview.AmountScreenClickIntentsStub
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.common.ui.amountScreen.ui.amountField
import com.tangem.common.ui.amountScreen.ui.buttons
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Amount screen with field
 * @param amountState amount state
 * @param isBalanceHiding flag hidden balances
 * @param clickIntents amount screen clicks
 */
@Composable
fun AmountScreenContent(amountState: AmountState, isBalanceHiding: Boolean, clickIntents: AmountScreenClickIntents) {
    if (amountState !is AmountState.Data) return

    // Do not put fillMaxSize() in here
    LazyColumn(
        modifier = Modifier
            .background(TangemTheme.colors.background.tertiary)
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        amountField(amountState = amountState, isBalanceHiding = isBalanceHiding)
        buttons(
            segmentedButtonConfig = amountState.segmentedButtonConfig,
            clickIntents = clickIntents,
            isSegmentedButtonsEnabled = amountState.isSegmentedButtonsEnabled,
            selectedButton = amountState.selectedButton,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendAmountContentPreview(
    @PreviewParameter(SendAmountContentPreviewProvider::class) amountState: AmountState,
) {
    TangemThemePreview {
        AmountScreenContent(
            amountState = amountState,
            isBalanceHiding = false,
            clickIntents = AmountScreenClickIntentsStub,
        )
    }
}

private class SendAmountContentPreviewProvider : PreviewParameterProvider<AmountState> {
    override val values: Sequence<AmountState>
        get() = sequenceOf(
            AmountStatePreviewData.amountState,
        )
}
// endregion