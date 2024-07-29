package com.tangem.features.markets.details.impl.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.features.markets.details.impl.ui.entity.MarketsTokenDetailsUM

@Suppress("UnusedPrivateMember")
@Composable
internal fun MarketsTokenDetailsContent(
    state: MarketsTokenDetailsUM,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
// [REDACTED_TODO_COMMENT]
}

@Suppress("UnusedPrivateMember")
@Composable
private fun Content(state: MarketsTokenDetailsUM, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column {
        TangemTopAppBar(
            title = state.tokenName,
            startButton = TopAppBarButtonUM.Back(onBackClick),
        )
    }
// [REDACTED_TODO_COMMENT]
}
