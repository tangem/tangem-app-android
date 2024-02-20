package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.animation.AnimatedContent
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
import com.tangem.common.Strings.STARS
import com.tangem.core.ui.components.currency.tokenicon.TokenIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendStates

@Composable
internal fun AmountFieldContainer(
    amountState: SendStates.AmountState,
    isBalanceHiding: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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
                .padding(top = TangemTheme.dimens.spacing14),
        )

        val balance = if (isBalanceHiding) STARS else amountState.walletBalance.resolveReference()
        AnimatedContent(
            targetState = balance,
            label = "Hide Balance Animation",
        ) {
            Text(
                text = it,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing2),
            )
        }
        TokenIcon(
            state = amountState.tokenIconState,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing32),
        )
        AmountField(
            sendField = amountState.amountTextField,
            isFiat = amountState.amountTextField.isFiatValue,
        )
    }
}
