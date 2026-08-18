package com.tangem.features.collectibles.impl.main.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/** Placeholder for the Collectibles main screen: the content lands in a separate task. */
@Composable
internal fun CollectiblesMainScreen(modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = { TangemTopNavigation() },
        content = { },
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesMainScreenPreview() {
    TangemThemePreviewRedesign {
        CollectiblesMainScreen()
    }
}