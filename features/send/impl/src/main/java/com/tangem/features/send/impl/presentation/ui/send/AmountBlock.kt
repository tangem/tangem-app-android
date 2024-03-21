package com.tangem.features.send.impl.presentation.ui.send

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.components.currency.tokenicon.TokenIcon
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendStates

@Composable
internal fun AmountBlock(
    amountState: SendStates.AmountState,
    isSuccess: Boolean,
    isEditingDisabled: Boolean,
    onClick: () -> Unit,
) {
    val amount = amountState.amountTextField

    val cryptoAmount = getAmountWithSymbol(amount.value, amount.cryptoAmount.currencySymbol)
    val fiatAmount = getAmountWithSymbol(amount.fiatValue, amount.fiatAmount.currencySymbol)
    val backgroundColor = if (isEditingDisabled) {
        TangemTheme.colors.button.disabled
    } else {
        TangemTheme.colors.background.action
    }

    val (firstAmount, secondAmount) = if (amount.isFiatValue) {
        fiatAmount to cryptoAmount
    } else {
        cryptoAmount to fiatAmount
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(backgroundColor)
            .clickable(enabled = !isSuccess && !isEditingDisabled, onClick = onClick)
            .padding(
                vertical = TangemTheme.dimens.spacing14,
                horizontal = TangemTheme.dimens.spacing16,
            ),
    ) {
        TokenIcon(state = amountState.tokenIconState)
        ResizableText(
            text = firstAmount,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens.spacing16),
        )
        Text(
            text = secondAmount,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens.spacing4),
        )
    }
}

private fun getAmountWithSymbol(amount: String, symbol: String): String {
    return "$amount $symbol"
}