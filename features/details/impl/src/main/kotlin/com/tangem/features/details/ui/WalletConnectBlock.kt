package com.tangem.features.details.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.details.impl.R

@Composable
internal fun WalletConnectBlock(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_wallet_connect_24),
                tint = TangemColorPalette.Azure,
                contentDescription = null,
            )

            Column(
                modifier = Modifier.heightIn(min = TangemTheme.dimens.size48),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                Text(
                    text = stringResourceSafe(id = R.string.wallet_connect_title),
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = stringResourceSafe(id = R.string.wallet_connect_subtitle),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}