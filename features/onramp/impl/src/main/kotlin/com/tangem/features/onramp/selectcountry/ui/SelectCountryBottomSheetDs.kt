package com.tangem.features.onramp.selectcountry.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_control_checkmark_20
import com.tangem.core.ui.test.SelectCountryBottomSheetTestTags
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
@RemoveWithToggle(
    toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED",
    description = "Rename over the old file",
)
internal fun OnrampCountryListDs(state: CountryListUM, onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        TangemModal.TopNavigation(
            title = resourceReference(R.string.tangempay_order_data_country),
            onClose = onCloseClick,
        )
        Box(modifier = Modifier.weight(1f).windowInsetsPadding(WindowInsets.ime)) {
            when (state) {
                is CountryListUM.Content -> CountryListContent(state = state)
                is CountryListUM.Error -> CountryListError(state = state)
                is CountryListUM.Loading -> CountryListLoading(state = state)
            }
            if (state is CountryListUM.Content) {
                CountrySearchFooter(
                    state = state.searchBarUM.toSearchState(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun CountryListContent(state: CountryListUM.Content, modifier: Modifier = Modifier) {
    val selectedItems = state.items.filter { it is CountryItemState.WithContent.Content && it.isSelected }
    val otherItems = state.items.filterNot { it in selectedItems }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(SelectCountryBottomSheetTestTags.LAZY_LIST),
        contentPadding = PaddingValues(bottom = FOOTER_INSET + navigationBarBottomInset()),
    ) {
        if (selectedItems.isNotEmpty()) {
            item(key = "selected_header") {
                SectionHeader(title = stringResourceSafe(R.string.tangempay_withdraw_bank_geo_country_selected))
            }
            items(items = selectedItems, key = { it.id }, contentType = { it::class.java }) { item ->
                CountryRow(item = item)
            }
        }
        if (otherItems.isNotEmpty()) {
            item(key = "others_header") {
                SectionHeader(title = stringResourceSafe(R.string.tangempay_residency_all_country))
            }
            items(items = otherItems, key = { it.id }, contentType = { it::class.java }) { item ->
                CountryRow(item = item)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        text = title,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Composable
private fun CountryRow(item: CountryItemState.WithContent) {
    when (item) {
        is CountryItemState.WithContent.Content -> TangemRow(
            modifier = Modifier.testTag(SelectCountryBottomSheetTestTags.COUNTRY_ITEM),
            verticalAlignment = TangemRowVerticalAlignment.Center,
            onClick = item.onClick,
            startSlot = {
                CountryFlag(
                    flagUrl = item.flagUrl,
                    modifier = Modifier.testTag(SelectCountryBottomSheetTestTags.COUNTRY_ICON),
                )
            },
            titleSlot = {
                TangemRowText(
                    text = item.countryName,
                    role = TangemRowTextRole.Title,
                    modifier = Modifier.testTag(SelectCountryBottomSheetTestTags.COUNTRY_NAME),
                )
            },
            endSlot = if (item.isSelected) {
                {
                    Icon(
                        imageVector = Icons.ic_control_checkmark_20,
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.primary,
                    )
                }
            } else {
                null
            },
        )
        is CountryItemState.WithContent.Unavailable -> TangemRow(
            modifier = Modifier.testTag(SelectCountryBottomSheetTestTags.UNAVAILABLE_COUNTRY_ITEM),
            verticalAlignment = TangemRowVerticalAlignment.Center,
            startSlot = {
                CountryFlag(
                    flagUrl = item.flagUrl,
                    modifier = Modifier
                        .alpha(UNAVAILABLE_ALPHA)
                        .testTag(SelectCountryBottomSheetTestTags.UNAVAILABLE_COUNTRY_ICON),
                )
            },
            titleSlot = {
                Text(
                    text = item.countryName,
                    maxLines = 1,
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.tertiary,
                )
            },
            endSlot = {
                Text(
                    text = stringResourceSafe(R.string.onramp_country_unavailable),
                    maxLines = 1,
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            },
        )
    }
}

@Composable
private fun CountryFlag(flagUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = flagUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(FLAG_SIZE)
            .clip(CircleShape)
            .background(color = TangemTheme.colors3.bg.transparent.primary),
    )
}

@Composable
private fun CountryListLoading(state: CountryListUM.Loading, modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        Column(modifier = modifier.fillMaxSize()) {
            state.items.forEach { _ ->
                TangemRow(
                    verticalAlignment = TangemRowVerticalAlignment.Center,
                    startSlot = {
                        TangemShimmer(modifier = Modifier.size(FLAG_SIZE), radius = FLAG_SIZE / 2)
                    },
                    titleSlot = {
                        TangemShimmer(
                            style = TangemTheme.typography3.body.medium,
                            modifier = Modifier.weight(weight = 1f),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CountryListError(state: CountryListUM.Error, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(id = R.string.markets_loading_error_title),
            textAlign = TextAlign.Center,
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X8,
            text = resourceReference(id = R.string.try_to_load_data_again_button_title),
            onClick = state.onRetry,
        )
    }
}

@Composable
private fun navigationBarBottomInset(): Dp = WindowInsets.navigationBars
    .exclude(WindowInsets.ime)
    .asPaddingValues()
    .calculateBottomPadding()

@Composable
private fun CountrySearchFooter(state: TangemSearch.State, modifier: Modifier = Modifier) {
    val navigationBarInset = navigationBarBottomInset()

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        TangemFade(
            position = TangemFade.Position.Bottom,
            variant = TangemFade.Variant.Hard,
            modifier = Modifier
                .fillMaxWidth()
                .height(FADE_HEIGHT + navigationBarInset),
        )
        TangemSearch(
            state = state,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp + navigationBarInset),
        )
    }
}

private fun SearchBarUM.toSearchState() = TangemSearch.State(
    placeholderText = placeholderText,
    query = query,
    onQueryChange = onQueryChange,
    isActive = isActive,
    onActiveChange = onActiveChange,
    onClearClick = onClearClick,
)

private val FLAG_SIZE = 40.dp
private val FOOTER_INSET = 88.dp
private val FADE_HEIGHT = 112.dp
private const val UNAVAILABLE_ALPHA = 0.4f

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_OnrampCountryList() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            OnrampCountryListDs(
                state = CountryListUM.Content(
                    searchBarUM = previewSearchBar(),
                    items = previewItems(),
                ),
                onCloseClick = {},
            )
        }
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_OnrampCountryListLoading() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            OnrampCountryListDs(
                state = CountryListUM.Loading(
                    searchBarUM = previewSearchBar(),
                    items = persistentListOf(
                        CountryItemState.Loading("1"),
                        CountryItemState.Loading("2"),
                        CountryItemState.Loading("3"),
                    ),
                ),
                onCloseClick = {},
            )
        }
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_OnrampCountryListError() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            OnrampCountryListDs(
                state = CountryListUM.Error(searchBarUM = previewSearchBar(), onRetry = {}),
                onCloseClick = {},
            )
        }
    }
}

private fun previewSearchBar() = SearchBarUM(
    placeholderText = resourceReference(R.string.onramp_country_search),
    query = "",
    onQueryChange = {},
    isActive = false,
    onActiveChange = {},
)

private fun previewItems(): ImmutableList<CountryItemState.WithContent> = persistentListOf(
    CountryItemState.WithContent.Content(
        id = "GE",
        flagUrl = "",
        countryName = "Georgia",
        isSelected = true,
        onClick = {},
    ),
    CountryItemState.WithContent.Content(
        id = "AD",
        flagUrl = "",
        countryName = "Andorra",
        isSelected = false,
        onClick = {},
    ),
    CountryItemState.WithContent.Unavailable(id = "AF", flagUrl = "", countryName = "Afghanistan"),
)