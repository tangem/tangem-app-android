package com.tangem.feature.wallet.presentation.common.component

import android.content.res.Configuration
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
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.impl.R
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

@Composable
internal fun NetworkGroupItem(networkName: String, modifier: Modifier = Modifier) {
    InternalNetworkGroupItem(modifier = modifier, networkName = networkName)
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

// TODO: https://tangem.atlassian.net/browse/AND-8044
@Composable
private fun InternalNetworkGroupItem(
    networkName: String,
    modifier: Modifier = Modifier,
    endIcon: @Composable (RowScope.() -> Unit)? = null,
) {
    val minHeight = if (endIcon == null) TangemTheme.dimens.size36 else TangemTheme.dimens.size40
    val padding = if (endIcon == null) {
        PaddingValues(
            start = TangemTheme.dimens.spacing12,
            top = TangemTheme.dimens.spacing12,
            end = TangemTheme.dimens.spacing12,
            bottom = TangemTheme.dimens.spacing4,
        )
    } else {
        PaddingValues(
            start = TangemTheme.dimens.spacing12,
            top = TangemTheme.dimens.spacing11,
            end = TangemTheme.dimens.spacing12,
            bottom = TangemTheme.dimens.spacing5,
        )
    }

    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(paddingValues = padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.wallet_network_group_title, networkName),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )

        if (endIcon != null) endIcon()
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
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NetworkGroupItemPreview(@PreviewParameter(NetworkGroupProvider::class) isDraggable: Boolean) {
    TangemThemePreview {
        NetworkGroupItemSample(isDraggable)
    }
}

private class NetworkGroupProvider : CollectionPreviewParameterProvider<Boolean>(
    collection = listOf(true, false),
)
// endregion Preview
