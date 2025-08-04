package com.tangem.features.onramp.selectcountry.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SelectCountryBottomSheetTestTags
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM

@Composable
internal fun SelectCountryBottomSheet(config: TangemBottomSheetConfig, content: @Composable () -> Unit) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = config,
        content = { content() },
    )
}

@Composable
internal fun OnrampCountryList(state: CountryListUM, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.testTag(SelectCountryBottomSheetTestTags.LAZY_LIST),
        contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
    ) {
        item(key = "search_bar") {
            SearchBar(
                state = state.searchBarUM,
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing20),
            )
        }
        when (state) {
            is CountryListUM.Content -> countryListWithContent(state = state)
            is CountryListUM.Error -> countryListError(state = state)
            is CountryListUM.Loading -> countryListLoading(state = state)
        }
    }
}

private fun LazyListScope.countryListError(state: CountryListUM.Error) {
    item(key = "error_content") {
        Column(
            modifier = Modifier
                .fillParentMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResourceSafe(id = R.string.markets_loading_error_title),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.tertiary,
            )
            SecondarySmallButton(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
                config = SmallButtonConfig(
                    text = resourceReference(id = R.string.try_to_load_data_again_button_title),
                    onClick = state.onRetry,
                ),
            )
        }
    }
}

private fun LazyListScope.countryListLoading(state: CountryListUM.Loading) {
    items(
        items = state.items,
        key = { item -> item.id },
        contentType = { item -> item::class.java },
        itemContent = { _ -> LoadingCountryItem(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing16)) },
    )
}

private fun LazyListScope.countryListWithContent(state: CountryListUM.Content) {
    items(
        items = state.items,
        key = { item -> item.id },
        contentType = { item -> item::class.java },
        itemContent = { item ->
            when (item) {
                is CountryItemState.WithContent.Content -> ContentCountryItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(vertical = TangemTheme.dimens.spacing16)
                        .testTag(SelectCountryBottomSheetTestTags.COUNTRY_ITEM),
                    state = item,
                )
                is CountryItemState.WithContent.Unavailable -> UnavailableCountryItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TangemTheme.dimens.spacing16)
                        .testTag(SelectCountryBottomSheetTestTags.UNAVAILABLE_COUNTRY_ITEM),
                    state = item,
                )
            }
        },
    )
}

@Composable
private fun LoadingCountryItem(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12)) {
        CircleShimmer(modifier = Modifier.size(TangemTheme.dimens.size36))
        TextShimmer(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(width = TangemTheme.dimens.size70),
            style = TangemTheme.typography.subtitle2,
            radius = TangemTheme.dimens.radius4,
        )
    }
}

@Composable
private fun ContentCountryItem(state: CountryItemState.WithContent.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier
                .size(TangemTheme.dimens.size36)
                .testTag(SelectCountryBottomSheetTestTags.COUNTRY_ICON),
            model = state.flagUrl,
            contentDescription = null,
        )
        Text(
            text = state.countryName,
            modifier = Modifier
                .weight(1F)
                .testTag(SelectCountryBottomSheetTestTags.COUNTRY_NAME),
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
@Suppress("MagicNumber")
private fun UnavailableCountryItem(state: CountryItemState.WithContent.Unavailable, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier
                .size(TangemTheme.dimens.size36)
                .alpha(0.4F)
                .testTag(SelectCountryBottomSheetTestTags.UNAVAILABLE_COUNTRY_ICON),
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
            text = stringResourceSafe(R.string.onramp_country_unavailable),
            maxLines = 1,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}