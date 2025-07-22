package com.tangem.features.walletconnect.transaction.ui.approve

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tangem.features.walletconnect.impl.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM
import com.tangem.features.walletconnect.transaction.ui.common.WcSmallTitleItem

@Composable
internal fun WcSpendAllowanceItem(
    spendAllowance: WcSpendAllowanceUM,
    onClickAllowToSpend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors.background.action)
            .fillMaxWidth()
            .padding(end = 12.dp, bottom = 12.dp)
            .clickable { onClickAllowToSpend() },
    ) {
        WcSmallTitleItem(R.string.wc_allow_to_spend)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                AsyncImage(
                    model = spendAllowance.tokenImageUrl,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(24.dp)
                        .clip(CircleShape),
                    contentDescription = null,
                )

                Spacer(modifier = Modifier.width(8.dp))

                AnimatedContent(
                    targetState = "${spendAllowance.amountText.resolveReference()} ${spendAllowance.tokenSymbol}",
                    label = "Animate Spend allowance text",
                ) { amount ->
                    Text(
                        text = amount,
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.primary1,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResourceSafe(R.string.manage_tokens_edit),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_new_12),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.informative,
                )
            }
        }
    }
}