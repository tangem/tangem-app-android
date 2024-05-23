package com.tangem.features.details.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.impl.R

@Composable
internal fun WalletConnectBlock(component: WalletConnectComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsStateWithLifecycle()

    Content(
        modifier = modifier,
        state = state as? WalletConnectComponent.State.Content ?: return,
    )
}

@Composable
private fun Content(state: WalletConnectComponent.State.Content, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        onClick = state.onClick,
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
                    text = stringResource(id = R.string.wallet_connect_title),
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = stringResource(id = R.string.wallet_connect_subtitle),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}
