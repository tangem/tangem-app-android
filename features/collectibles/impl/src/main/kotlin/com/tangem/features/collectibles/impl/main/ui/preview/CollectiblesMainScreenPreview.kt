package com.tangem.features.collectibles.impl.main.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.discovery.ui.preview.CollectiblesDiscoveryPreviewComponent
import com.tangem.features.collectibles.impl.discovery.ui.preview.CollectiblesDiscoveryPreviewData
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.features.collectibles.impl.main.ui.CollectiblesMainScreen
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesMainScreenPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.default)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenZeroPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(
            state = CollectiblesMainPreviewData.zero,
            discoveryState = CollectiblesDiscoveryPreviewData.default,
        )
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenListErrorPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.listError)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchActivePreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.searchActive)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchZeroPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.searchZero)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchErrorPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.searchError)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenMenuPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(
            state = CollectiblesMainPreviewData.menuShown,
            discoveryState = CollectiblesDiscoveryPreviewData.default,
        )
    }
}

@Composable
private fun PreviewScreen(
    discoveryState: CollectiblesDiscoveryUM,
    state: CollectiblesMainUM = CollectiblesMainPreviewData.default,
) {
    CollectiblesMainScreen(
        state = state,
        pullToRefresh = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
        discoveryComponent = remember { CollectiblesDiscoveryPreviewComponent(um = discoveryState) },
        discoveryState = discoveryState,
    )
}