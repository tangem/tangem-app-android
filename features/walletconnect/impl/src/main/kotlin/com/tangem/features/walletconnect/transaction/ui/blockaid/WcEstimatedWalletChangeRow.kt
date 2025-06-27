package com.tangem.features.walletconnect.transaction.ui.blockaid

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM

@Composable
internal fun WcEstimatedWalletChangeRow(item: WcEstimatedWalletChangeUM, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(id = item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title.resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = item.description,
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        AsyncImage(
            model = item.tokenIconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp)),
            placeholder = painterResource(R.drawable.ic_nft_placeholder_20),
            error = painterResource(R.drawable.ic_nft_placeholder_20),
        )
    }
}