package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.currency.tokenicon.TokenIcon
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendStates

@Composable
internal fun AmountFieldContainer(amountState: SendStates.AmountState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = TangemTheme.dimens.spacing4,
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
            )
            .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
            .background(TangemTheme.colors.background.action),
    ) {
        Text(
            text = amountState.walletName,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing14)
                .align(Alignment.CenterHorizontally),
        )
        Text(
            text = amountState.walletBalance,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing2)
                .align(Alignment.CenterHorizontally),
        )
        TokenIcon(
            state = amountState.tokenIconState,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing32)
                .align(Alignment.CenterHorizontally),
        )
        AmountField(
            sendField = amountState.amountTextField,
            isFiat = amountState.isFiatValue,
            cryptoSymbol = amountState.cryptoCurrencyStatus.currency.symbol,
            fiatSymbol = amountState.appCurrency.symbol,
        )
    }
}