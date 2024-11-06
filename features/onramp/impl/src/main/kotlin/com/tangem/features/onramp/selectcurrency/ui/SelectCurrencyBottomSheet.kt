package com.tangem.features.onramp.selectcurrency.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImage
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesListUM
import com.tangem.features.onramp.selectcurrency.entity.CurrencyItemState

@Composable
internal fun SelectCurrencyBottomSheet(config: TangemBottomSheetConfig, content: @Composable () -> Unit) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = config,
        content = { content() },
    )
}

@Composable
internal fun OnrampCurrencyList(state: CurrenciesListUM, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
    ) {
        item(key = "search_bar") {
            SearchBar(
                state = state.searchBarUM,
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing20),
            )
        }
        when (state) {
            is CurrenciesListUM.Content -> currencyListContent(state = state)
            is CurrenciesListUM.Error -> TODO()
            is CurrenciesListUM.Loading -> currencyListLoading(state = state)
        }
    }
}

private fun LazyListScope.currencyListLoading(state: CurrenciesListUM.Loading) {
    state.sections.fastForEach { section ->
        item(key = section.title.toString()) { GroupTitle(title = section.title.resolveReference()) }
        items(
            items = section.items,
            key = { item -> item.id },
            contentType = { item -> item::class.java },
            itemContent = { CurrencyLoading(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing16)) },
        )
    }
}

private fun LazyListScope.currencyListContent(state: CurrenciesListUM.Content) {
    state.sections.fastForEach { section ->
        item(key = section.title.toString()) { GroupTitle(title = section.title.resolveReference()) }
        items(
            items = section.items,
            key = { item -> item.id },
            contentType = { item -> item::class.java },
            itemContent = { item ->
                CurrencyContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(vertical = TangemTheme.dimens.spacing16),
                    currency = item,
                )
            },
        )
    }
}

@Composable
private fun GroupTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = title,
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
private fun CurrencyContent(currency: CurrencyItemState.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier
                .size(TangemTheme.dimens.size36)
                .clip(CircleShape),
            model = currency.onrampCurrency.image,
            contentDescription = null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2)) {
            Text(
                text = currency.onrampCurrency.code,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = currency.onrampCurrency.name,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun CurrencyLoading(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(modifier = Modifier.size(TangemTheme.dimens.size36))
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6)) {
            TextShimmer(
                modifier = Modifier.width(width = TangemTheme.dimens.size70),
                style = TangemTheme.typography.subtitle2,
            )
            TextShimmer(
                modifier = Modifier.width(width = TangemTheme.dimens.size52),
                style = TangemTheme.typography.caption2,
            )
        }
    }
}
