package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState

@Composable
internal fun TangemPayCardMainBlock(state: TangemPayState.Card, modifier: Modifier = Modifier) {
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
                painter = painterResource(R.drawable.img_tangem_pay_card_main_36),
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape),
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResourceSafe(R.string.tangempay_kyc_card_ready_notification_title),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.img_visa_label_26_16),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp, 16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = state.lastFourDigits.resolveReference(),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.TopEnd,
            ) {
                Text(
                    text = state.balanceText.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}