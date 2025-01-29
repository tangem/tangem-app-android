package com.tangem.core.ui.components.containers.pullToRefresh

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TangemPullToRefreshContainer(
    config: PullToRefreshConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = config.isRefreshing,
        onRefresh = {
            config.onRefresh(PullToRefreshConfig.ShowRefreshState())
        },
        state = state,
        modifier = modifier,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = config.isRefreshing,
                state = state,
                containerColor = TangemTheme.colors.background.tertiary,
                color = TangemTheme.colors.text.primary1,
            )
        },
    ) {
        content()
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPullToRefreshContainer_Preview() {
    TangemThemePreview {
        TangemPullToRefreshContainer(
            config = PullToRefreshConfig(isRefreshing = true, {}),
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(TangemTheme.colors.background.secondary),
            )
        }
    }
}
// endregion