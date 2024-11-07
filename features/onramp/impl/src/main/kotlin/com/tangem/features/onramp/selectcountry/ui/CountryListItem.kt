package com.tangem.features.onramp.selectcountry.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.entity.CountriesListItemUM
import com.tangem.features.onramp.selectcountry.entity.CountryItemState

@Composable
internal fun CountryListItem(state: CountriesListItemUM, modifier: Modifier = Modifier) {
    when (state) {
        is CountriesListItemUM.Country -> CountryItem(
            state = state.state,
            modifier = modifier
                .countryClickable(state.state)
                .padding(all = TangemTheme.dimens.spacing16),
        )
        is CountriesListItemUM.SearchBar -> SearchBar(
            state = state.searchBarUM,
            modifier = modifier.padding(all = TangemTheme.dimens.spacing16),
        )
    }
}

@Composable
private fun CountryItem(state: CountryItemState, modifier: Modifier = Modifier) {
    when (state) {
        is CountryItemState.Content -> ContentCountryItem(state = state, modifier = modifier)
        is CountryItemState.Loading -> LoadingCountryItem(modifier)
        is CountryItemState.Unavailable -> UnavailableCountryItem(state = state, modifier = modifier)
    }
}

@Composable
private fun ContentCountryItem(state: CountryItemState.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier.size(TangemTheme.dimens.size36),
            model = state.flagUrl,
            contentDescription = null,
        )
        Text(
            text = state.countryName,
            modifier = Modifier.weight(1F),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
        )
        if (state.isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_24),
                contentDescription = null,
                modifier = Modifier.size(TangemTheme.dimens.size24),
                tint = TangemTheme.colors.icon.accent,
            )
        }
    }
}

@Composable
private fun LoadingCountryItem(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12)) {
        CircleShimmer(modifier = Modifier.size(TangemTheme.dimens.size36))
        RectangleShimmer(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(width = TangemTheme.dimens.size70, height = TangemTheme.dimens.size12),
            radius = TangemTheme.dimens.radius4,
        )
    }
}

@Composable
@Suppress("MagicNumber")
private fun UnavailableCountryItem(state: CountryItemState.Unavailable, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier
                .size(TangemTheme.dimens.size36)
                .alpha(0.4F),
            model = state.flagUrl,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1F),
            text = state.countryName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.disabled,
        )
        Text(
            text = "Unavailable",
            maxLines = 1,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.countryClickable(state: CountryItemState): Modifier = composed {
    when (state) {
        is CountryItemState.Content -> combinedClickable(onClick = state.onClick)
        is CountryItemState.Loading,
        is CountryItemState.Unavailable,
        -> this
    }
}