package com.tangem.features.polymarket.impl.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent

/**
 * Placeholder search screen. Real UI arrives in a later task.
 */
internal class PolymarketSearchComponent(
    appComponentContext: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(text = "Search")
        }
    }
}