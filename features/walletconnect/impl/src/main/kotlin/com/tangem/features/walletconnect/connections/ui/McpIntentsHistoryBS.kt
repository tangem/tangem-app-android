package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.features.walletconnect.connections.entity.McpIntentsHistoryUM
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.persistentListOf

/**
 * [Show in Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=25303-383848&m=dev)
 * [Loading in Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=32418-418763&m=dev)
 */
@Composable
internal fun McpIntentsHistoryBS(state: McpIntentsHistoryUM) {
    TangemBottomSheet<McpIntentsHistoryUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = state,
        ),
        type = TangemBottomSheetType.Modal,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.mcp_intents_history_title),
                subtitle = stringReference(state.agentName),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = state.onDismiss,
            )
        },
        content = { McpIntentsHistoryContent(state = it.content) },
    )
}

@Composable
private fun McpIntentsHistoryContent(state: McpIntentsHistoryUM.Content, modifier: Modifier = Modifier) {
    when (state) {
        McpIntentsHistoryUM.Content.Loading -> LoadingContent(modifier)
        is McpIntentsHistoryUM.Content.Data -> DataContent(state = state, modifier = modifier)
    }
}

@Composable
private fun DataContent(state: McpIntentsHistoryUM.Content.Data, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        state.groups.forEach { group ->
            item(key = "title_${group.key}") {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    text = group.title.resolveReference(),
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
            items(items = group.rows, key = { it.key }) { row -> IntentRow(row = row) }
        }
        item(key = "next_page") {
            NextPageContent(nextPage = state.nextPage)
            Spacer(modifier = Modifier.height(LocalTangemBottomSheetContentBottomInset.current))
        }
    }
}

@Composable
private fun NextPageContent(nextPage: McpIntentsHistoryUM.NextPage) {
    when (nextPage) {
        McpIntentsHistoryUM.NextPage.End -> Unit
        McpIntentsHistoryUM.NextPage.Loading -> ShimmerRow()
        is McpIntentsHistoryUM.NextPage.Ready -> LaunchedEffect(nextPage) { nextPage.onLoadMore() }
        is McpIntentsHistoryUM.NextPage.Error -> TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Ghost,
            text = resourceReference(R.string.common_retry),
            onClick = nextPage.onRetry,
        )
    }
}

@Composable
private fun IntentRow(row: McpIntentsHistoryUM.Row, modifier: Modifier = Modifier) {
    val title = when (row.kind) {
        McpIntentsHistoryUM.Kind.Swap -> stringResourceSafe(R.string.common_swap)
        McpIntentsHistoryUM.Kind.Send -> stringResourceSafe(R.string.common_send)
        McpIntentsHistoryUM.Kind.AgentConnected -> stringResourceSafe(R.string.mcp_connection_success_title)
    }

    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            TangemAvatar(
                state = TangemAvatar.UiState.Icon(
                    imageVector = when (row.kind) {
                        McpIntentsHistoryUM.Kind.Swap -> Icons.ic_arrow_swap_horizontal_24
                        McpIntentsHistoryUM.Kind.Send -> Icons.ic_arrow_up_24
                        McpIntentsHistoryUM.Kind.AgentConnected -> Icons.ic_lightning_24
                    },
                    appearance = TangemAvatar.Appearance.Neutral,
                ),
                size = TangemAvatar.Size.X40,
            )
        },
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = row.time, role = TangemRowTextRole.Subtitle) },
        endSlot = row.status?.let { status ->
            {
                TangemBadge(
                    text = resourceReference(
                        when (status) {
                            McpIntentsHistoryUM.Status.New -> R.string.mcp_intents_history_status_new
                            McpIntentsHistoryUM.Status.Viewed -> R.string.mcp_intents_history_status_viewed
                        },
                    ),
                    status = when (status) {
                        McpIntentsHistoryUM.Status.New -> TangemBadge.Status.Info
                        McpIntentsHistoryUM.Status.Viewed -> TangemBadge.Status.Neutral
                    },
                    size = TangemBadge.Size.X6,
                )
            }
        },
        onClick = row.onClick,
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(vertical = 12.dp)) {
        RectangleShimmer(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).size(width = 80.dp, height = 18.dp),
        )
        repeat(times = SHIMMER_ROWS) { ShimmerRow() }
    }
}

@Composable
private fun ShimmerRow(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { CircleShimmer(modifier = Modifier.size(40.dp)) },
        titleSlot = { RectangleShimmer(modifier = Modifier.size(width = 120.dp, height = 16.dp)) },
        subtitleSlot = { RectangleShimmer(modifier = Modifier.size(width = 60.dp, height = 12.dp)) },
    )
}

private const val SHIMMER_ROWS = 3

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpIntentsHistoryBSPreview() {
    TangemThemePreviewRedesign { McpIntentsHistoryBS(state = previewState) }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpIntentsHistoryContentPreview() {
    TangemThemePreviewRedesign {
        Box(contentAlignment = Alignment.TopStart) { McpIntentsHistoryContent(state = previewState.content) }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpIntentsHistoryLoadingPreview() {
    TangemThemePreviewRedesign { McpIntentsHistoryContent(state = McpIntentsHistoryUM.Content.Loading) }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpIntentsHistoryNextPageLoadingPreview() {
    TangemThemePreviewRedesign {
        McpIntentsHistoryContent(state = previewData.copy(nextPage = McpIntentsHistoryUM.NextPage.Loading))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpIntentsHistoryNextPageErrorPreview() {
    TangemThemePreviewRedesign {
        McpIntentsHistoryContent(state = previewData.copy(nextPage = McpIntentsHistoryUM.NextPage.Error(onRetry = {})))
    }
}

private val previewData = McpIntentsHistoryUM.Content.Data(
    groups = persistentListOf(
        McpIntentsHistoryUM.DayGroup(
            key = "2026-11-12",
            title = stringReference("Today"),
            rows = persistentListOf(
                previewRow(
                    key = "1",
                    kind = McpIntentsHistoryUM.Kind.Swap,
                    time = "4:40 PM",
                    status = McpIntentsHistoryUM.Status.New,
                ),
                previewRow(
                    key = "2",
                    kind = McpIntentsHistoryUM.Kind.Send,
                    time = "12:24 AM",
                    status = McpIntentsHistoryUM.Status.Viewed,
                ),
            ),
        ),
        McpIntentsHistoryUM.DayGroup(
            key = "2026-10-04",
            title = stringReference("Oct 4"),
            rows = persistentListOf(
                previewRow(
                    key = "3",
                    kind = McpIntentsHistoryUM.Kind.AgentConnected,
                    time = "3:12 PM",
                    status = null,
                ),
            ),
        ),
    ),
    nextPage = McpIntentsHistoryUM.NextPage.End,
)

private val previewState = McpIntentsHistoryUM(
    agentName = "Claude Agent",
    content = previewData,
    onDismiss = {},
)

private fun previewRow(
    key: String,
    kind: McpIntentsHistoryUM.Kind,
    time: String,
    status: McpIntentsHistoryUM.Status?,
) = McpIntentsHistoryUM.Row(
    key = key,
    kind = kind,
    time = time,
    status = status,
    onClick = status?.let { {} },
)