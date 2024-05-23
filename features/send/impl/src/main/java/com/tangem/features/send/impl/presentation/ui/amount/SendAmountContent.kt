package com.tangem.features.send.impl.presentation.ui.amount

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.previewdata.AmountStatePreviewData
import com.tangem.features.send.impl.presentation.state.previewdata.SendClickIntentsStub
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents

@Composable
internal fun SendAmountContent(
    amountState: SendStates.AmountState?,
    isBalanceHiding: Boolean,
    clickIntents: SendClickIntents,
) {
    if (amountState == null) return
    // Do not put fillMaxSize() in here
    LazyColumn(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            )
            .background(TangemTheme.colors.background.tertiary),
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
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AmountFieldPreview(
    @PreviewParameter(AmountFieldPreviewProvider::class) amountState: SendStates.AmountState,
) {
    TangemThemePreview {
        SendAmountContent(
            amountState = amountState,
            isBalanceHiding = false,
            clickIntents = SendClickIntentsStub,
        )
    }
}

private class AmountFieldPreviewProvider : PreviewParameterProvider<SendStates.AmountState> {
    override val values: Sequence<SendStates.AmountState>
        get() = sequenceOf(
            AmountStatePreviewData.amountState,
        )
}
// endregion