package com.tangem.features.polymarket.impl.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute

/**
 * Placeholder Discovery feed screen. Real UI (event cards) arrives in [REDACTED_TASK_KEY]+.
 *
 * Navigation is performed via the [router] carried in the child [AppComponentContext], which is the feature's
 * inner router — so pushes stay inside the Polymarket stack.
 */
internal class PolymarketMainComponent(
    appComponentContext: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Predictions — Main")
            Text(
                text = "Open sample event",
                modifier = Modifier.clickable(onClick = ::onOpenSampleEvent),
            )
            Text(
                text = "Search",
                modifier = Modifier.clickable(onClick = ::onSearch),
            )
        }
    }

    private fun onOpenSampleEvent() {
        router.push(PolymarketRoute.EventDetails(eventId = "sample-event"))
    }

    private fun onSearch() {
        router.push(PolymarketRoute.Search)
    }
}