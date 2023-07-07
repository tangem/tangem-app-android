package com.tangem.feature.wallet.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

@Composable
internal fun NetworkGroupItem(networkName: String, modifier: Modifier = Modifier) {
    InternalNetworkGroupItem(
        modifier = modifier,
        networkName = networkName,
    )
}

@Composable
internal fun DraggableNetworkGroupItem(
    networkName: String,
    modifier: Modifier = Modifier,
    reorderableTokenListState: ReorderableLazyListState? = null,
) {
    InternalNetworkGroupItem(
        modifier = modifier,
        networkName = networkName,
        endIcon = {
            Box(
                modifier = Modifier
                    .size(TangemTheme.dimens.size32)
                    .let {
                        if (reorderableTokenListState != null) {
                            it.detectReorder(reorderableTokenListState)
                        } else {
                            it
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_group_drop_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun InternalNetworkGroupItem(
    networkName: String,
    modifier: Modifier = Modifier,
    endIcon: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(horizontal = TangemTheme.dimens.spacing14)
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size48),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.wallet_network_group_title, networkName),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            endIcon()
        }
    }
}

// region Preview
@Composable
private fun NetworkGroupItemSample(isDraggable: Boolean) {
    if (isDraggable) {
        DraggableNetworkGroupItem(networkName = "Ethereum")
    } else {
        NetworkGroupItem(networkName = "Ethereum")
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NetworkGroupItemPreview_Light(@PreviewParameter(NetworkGroupProvider::class) isDraggable: Boolean) {
    TangemTheme {
        NetworkGroupItemSample(isDraggable)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NetworkGroupItemPreview_Dark(@PreviewParameter(NetworkGroupProvider::class) isDraggable: Boolean) {
    TangemTheme(isDark = true) {
        NetworkGroupItemSample(isDraggable)
    }
}

private class NetworkGroupProvider : CollectionPreviewParameterProvider<Boolean>(
    collection = listOf(true, false),
)
// endregion Preview