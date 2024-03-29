package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendStates
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
        buttons(amountState.segmentedButtonConfig, clickIntents)
        notifications(amountState.notifications)
    }
}
