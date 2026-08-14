package com.tangem.features.polymarket.impl.entry.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Entry route content — a loader shown while the wallet is being settled, before the entry gate takes over.
 */
@Composable
internal fun PolymarketEntryScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader(
            color = TangemTheme.colors3.icon.primary,
            size = TangemLoaderSize.X32,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketEntryScreenPreview() {
    TangemThemePreviewRedesign {
        PolymarketEntryScreen()
    }
}