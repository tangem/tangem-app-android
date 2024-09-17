package com.tangem.feature.wallet.presentation.common.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.rows.NetworkTitle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.impl.R
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState

@Composable
internal fun NetworkTitleItem(networkName: String, modifier: Modifier = Modifier) {
    BaseNetworkTitleItem(networkName = networkName, modifier = modifier)
}

@Composable
internal fun DraggableNetworkTitleItem(
    networkName: String,
    reorderableTokenListState: ReorderableLazyListState,
    modifier: Modifier = Modifier,
) {
    BaseNetworkTitleItem(
        networkName = networkName,
        modifier = modifier,
        action = { DraggableIcon(reorderableTokenListState = reorderableTokenListState) },
    )
}

@Composable
private fun BaseNetworkTitleItem(
    networkName: String,
    modifier: Modifier = Modifier,
    action: (@Composable BoxScope.() -> Unit)? = null,
) {
    NetworkTitle(
        title = {
            Text(
                text = stringResource(id = R.string.wallet_network_group_title, networkName),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
        modifier = modifier,
        action = action,
    )
}

@Composable
private fun DraggableIcon(reorderableTokenListState: ReorderableLazyListState) {
    Box(
        modifier = Modifier
            .size(TangemTheme.dimens.size32)
            .detectReorder(reorderableTokenListState),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = rememberVectorPainter(
                image = ImageVector.vectorResource(id = R.drawable.ic_group_drop_24),
            ),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NetworkTitleItemPreview(@PreviewParameter(NetworkTitleItemProvider::class) isDraggable: Boolean) {
    TangemThemePreview {
        if (isDraggable) {
            DraggableNetworkTitleItem(
                networkName = "Ethereum",
                reorderableTokenListState = rememberReorderableLazyListState(onMove = { _, _ -> }),
                modifier = Modifier.background(color = TangemTheme.colors.background.primary),
            )
        } else {
            NetworkTitleItem(
                networkName = "Ethereum",
                modifier = Modifier.background(color = TangemTheme.colors.background.primary),
            )
        }
    }
}

private object NetworkTitleItemProvider : CollectionPreviewParameterProvider<Boolean>(collection = listOf(true, false))
