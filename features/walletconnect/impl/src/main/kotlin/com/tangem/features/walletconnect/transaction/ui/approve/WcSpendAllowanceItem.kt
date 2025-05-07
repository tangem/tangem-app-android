package com.tangem.features.walletconnect.transaction.ui.approve

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM
import com.tangem.features.walletconnect.transaction.ui.common.WcSmallTitleItem

@Composable
internal fun WcSpendAllowanceItem(spendAllowance: WcSpendAllowanceUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.action)
            .fillMaxWidth()
            .padding(
                end = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing12,
            ),
    ) {
        WcSmallTitleItem(R.string.wc_allow_to_spend)

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))

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
                        .size(TangemTheme.dimens.size24)
                        .clip(CircleShape),
                    contentDescription = null,
                )

                Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing12))

                Text(
                    text = spendAllowance.amountText,
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(R.string.manage_tokens_edit),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                )

                Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing8))

                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_new_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.informative,
                )
            }
        }
    }
}