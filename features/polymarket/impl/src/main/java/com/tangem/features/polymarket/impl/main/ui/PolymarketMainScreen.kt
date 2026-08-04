package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PolymarketMainScreen(state: PolymarketMainUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        when (val content = state.content) {
            PolymarketMainUM.ContentUM.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
            is PolymarketMainUM.ContentUM.Content -> ContentState(
                modifier = Modifier.fillMaxSize(),
                state = content,
            )
            PolymarketMainUM.ContentUM.Empty -> MessageState(
                modifier = Modifier.fillMaxSize(),
                text = "No events yet",
            )
            is PolymarketMainUM.ContentUM.Error -> ErrorState(
                modifier = Modifier.fillMaxSize(),
                onRetryClick = content.onRetryClick,
            )
        }
    }
}

@Composable
private fun ContentState(state: PolymarketMainUM.ContentUM.Content, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = state.events,
            key = { it.id },
        ) { event ->
            PolymarketEventCard(
                modifier = Modifier.fillMaxWidth(),
                state = event,
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader(
            color = TangemTheme.colors3.icon.primary,
            size = TangemLoaderSize.X32,
        )
    }
}

@Composable
private fun MessageState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
    }
}

@Composable
private fun ErrorState(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Something went wrong",
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        TangemButton(
            modifier = Modifier.padding(top = 16.dp),
            size = TangemButton.Size.X10,
            variant = TangemButton.Variant.Primary,
            text = stringReference("Retry"),
            onClick = onRetryClick,
        )
    }
}

@Preview(name = "Content Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Content Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainScreenContentPreview() {
    TangemThemePreviewRedesign {
        PolymarketMainScreen(
            state = PolymarketMainUM(
                accessMode = PolymarketAccessMode.TRADING,
                content = PolymarketMainUM.ContentUM.Content(previewEvents()),
            ),
        )
    }
}

@Preview(name = "States Light", showBackground = true, widthDp = 360)
@Preview(
    name = "States Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainScreenStatesPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TangemLoader(size = TangemLoaderSize.X24)
            }
            Text(
                text = "No events yet",
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.body.medium,
            )
            ErrorState(onRetryClick = {})
        }
    }
}

private fun previewEvents() = persistentListOf(
    PolymarketEventUM(
        id = "grouped",
        title = stringReference("World Cup winner 2026"),
        iconUrl = null,
        volume = stringReference("Total volume: $6.3M"),
        rows = persistentListOf(
            PolymarketEventRowUM(
                marketId = "france",
                title = stringReference("France"),
                probability = stringReference("24%"),
                outcomes = previewOutcomes(),
            ),
            PolymarketEventRowUM(
                marketId = "uzbekistan",
                title = stringReference("Uzbekistan"),
                probability = stringReference("3%"),
                outcomes = previewOutcomes(),
            ),
        ),
        hiddenMarketsCount = 4,
        onClick = {},
    ),
    PolymarketEventUM(
        id = "plain",
        title = stringReference("Will Ethereum reach $5,000 before the end of the year?"),
        iconUrl = null,
        volume = null,
        rows = persistentListOf(
            PolymarketEventRowUM(
                marketId = "probability",
                title = stringReference("Probability"),
                probability = stringReference("80%"),
                outcomes = previewOutcomes(),
            ),
        ),
        hiddenMarketsCount = 0,
        onClick = {},
    ),
)

private fun previewOutcomes() = persistentListOf(
    PolymarketOutcomeUM(
        assetId = "yes",
        title = stringReference("Yes"),
        onClick = {},
    ),
    PolymarketOutcomeUM(
        assetId = "no",
        title = stringReference("No"),
        onClick = {},
    ),
)