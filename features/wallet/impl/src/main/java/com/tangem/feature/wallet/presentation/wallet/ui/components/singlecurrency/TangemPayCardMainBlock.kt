package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState

@Composable
internal fun TangemPayCardMainBlock(
    state: TangemPayState.Card,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.background.primary,
        onClick = state.onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.img_visa_36),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResourceSafe(R.string.tangempay_payment_account),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = state.lastFourDigits.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = state.balanceText.resolveReference().orMaskWithStars(isBalanceHidden),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.End,
                )

                Text(
                    text = state.balanceSymbol.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}