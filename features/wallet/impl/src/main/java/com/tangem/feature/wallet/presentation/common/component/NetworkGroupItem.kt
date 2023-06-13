package com.tangem.feature.wallet.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.state.NetworkGroupState

@Composable
internal fun NetworkGroupItem(state: NetworkGroupState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(horizontal = TangemTheme.dimens.spacing12)
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size48),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.wallet_network_group_title, state.networkName),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            if (state is NetworkGroupState.Draggable) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_group_drop_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        }
        Column {
            state.tokens.forEach { token ->
                key(token.id) {
                    TokenItem(state = token)
                }
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NetworkGroupItemPreview_Light(@PreviewParameter(NetworkGroupProvider::class) group: NetworkGroupState) {
    TangemTheme {
        NetworkGroupItem(group)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NetworkGroupItemPreview_Dark(@PreviewParameter(NetworkGroupProvider::class) group: NetworkGroupState) {
    TangemTheme(isDark = true) {
        NetworkGroupItem(group)
    }
}

private class NetworkGroupProvider : CollectionPreviewParameterProvider<NetworkGroupState>(
    collection = listOf(
        WalletPreviewData.networkGroup,
        WalletPreviewData.draggableNetworkGroup,
    ),
)
// endregion Preview