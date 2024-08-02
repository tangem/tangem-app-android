package com.tangem.common.ui.amountScreen.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.utils.StringsSigns.STARS

private const val AMOUNT_FIELD_KEY = "amountFieldKey"

internal fun LazyListScope.amountField(
    amountState: AmountState.Data,
    isBalanceHiding: Boolean,
    modifier: Modifier = Modifier,
) {
    item(key = AMOUNT_FIELD_KEY) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
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
            CurrencyIcon(
                state = amountState.tokenIconState,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing32),
            )
            AmountField(
                amountField = amountState.amountTextField,
                appCurrencyCode = amountState.appCurrencyCode,
            )
        }
    }
}