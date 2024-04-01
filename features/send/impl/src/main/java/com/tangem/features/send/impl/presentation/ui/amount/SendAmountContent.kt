package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.previewdata.AmountStatePreviewData
import com.tangem.features.send.impl.presentation.state.previewdata.SendClickIntentsStub
import com.tangem.features.send.impl.presentation.ui.common.notifications
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents

@Composable
internal fun SendAmountContent(
    amountState: SendStates.AmountState?,
    isBalanceHiding: Boolean,
    clickIntents: SendClickIntents,
) {
    if (amountState == null) return
    LazyColumn(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .background(TangemTheme.colors.background.tertiary),
    ) {
        amountField(amountState = amountState, isBalanceHiding = isBalanceHiding)
        buttons(
            segmentedButtonConfig = amountState.segmentedButtonConfig,
            clickIntents = clickIntents,
            isMaxButtonEnabled = !amountState.isFeeLoading,
        )
        notifications(amountState.notifications)
    }
}

// region Preview
@Preview
@Composable
private fun AmountFieldPreview_Light(
    @PreviewParameter(AmountFieldPreviewProvider::class) amountState: SendStates.AmountState,
) {
    TangemTheme {
        SendAmountContent(
            amountState = amountState,
            isBalanceHiding = false,
            clickIntents = SendClickIntentsStub,
        )
    }
}

@Preview
@Composable
private fun AmountFieldPreview_Dark(
    @PreviewParameter(AmountFieldPreviewProvider::class) amountState: SendStates.AmountState,
) {
    TangemTheme(isDark = true) {
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