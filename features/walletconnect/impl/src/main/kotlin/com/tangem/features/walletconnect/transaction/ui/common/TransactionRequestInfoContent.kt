package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButtonIconEnd
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.WcSignTransactionUM

@Composable
internal fun TransactionRequestInfoContent(state: WcSignTransactionUM) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 310.dp)
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                .background(TangemTheme.colors.background.action)
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing20,
                ),
        ) {
            state.transactionRequestInfo.info.forEach { item ->
                Text(
                    text = item.title.resolveReference(),
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing20),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.tertiary,
                )
                Text(
                    text = item.description,
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing4),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }

        SecondaryButtonIconEnd(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = TangemTheme.dimens.spacing20)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.wc_copy_data_button_text),
            onClick = state.actions.onCopy,
            iconResId = R.drawable.ic_copy_24,
        )
    }
}