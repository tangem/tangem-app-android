package com.tangem.features.addressbook.list.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.features.addressbook.common.ui.ContactRow
import com.tangem.features.addressbook.list.ui.preview.AddressBookListPreviewParameterProvider
import com.tangem.features.addressbook.list.ui.preview.AddressBookListPreviewScenario
import com.tangem.features.addressbook.list.ui.state.AddressBookChipUM
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.list.ui.state.ContentMode
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AddressBookListScreen(
    state: AddressBookListUM.Content,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
    Column(modifier = modifier) {
        TangemTopBar(
            modifier = Modifier.statusBarsPadding(),
            title = resourceReference(R.string.address_book_title),
            startContent = when (state.contentMode) {
                is ContentMode.Default -> {
                    {
                        TangemButton(
                            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_chevron_left_20),
                            onClick = onBackClick,
                            size = TangemButton.Size.X11,
                            variant = TangemButton.Variant.Material,
                        )
                    }
                }
                ContentMode.Select -> null
            },
            endContent = {
                TangemButton(
                    iconStart = TangemIconUM.Icon(
                        imageVector = when (state.contentMode) {
                            is ContentMode.Default -> Icons.ic_sign_plus_20
                            ContentMode.Select -> Icons.ic_cross_20
                        },
                    ),
                    onClick = when (state.contentMode) {
                        is ContentMode.Default -> state.contentMode.onAddClick
                        ContentMode.Select -> onBackClick
                    },
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )

        TangemSearch(
            state = state.searchBar,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        if (state.chips.isNotEmpty()) {
            WalletChips(chips = state.chips)
        }

        if (state.isNothingFound) {
            NothingFoundContent()
        } else {
            LazyColumn(
                modifier = Modifier.imePadding(),
                contentPadding = PaddingValues(bottom = 12.dp + bottomBarHeight),
            ) {
                itemsIndexed(items = state.contacts, key = { _, contact -> contact.id }) { index, contact ->
                    ContactRow(
                        contact = contact,
                        modifier = Modifier.roundedShapeItemDecoration(
                            currentIndex = index,
                            lastIndex = state.contacts.lastIndex,
                            radius = 24.dp,
                            backgroundColor = TangemTheme.colors3.bg.secondary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletChips(chips: ImmutableList<AddressBookChipUM>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = chips, key = AddressBookChipUM::id) { chip ->
            AddressBookChip(state = chip)
        }
    }
}

@Composable
private fun ColumnScope.NothingFoundContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(color = TangemTheme.colors3.bg.opaque.primary, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_search_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = stringResourceSafe(R.string.common_no_results),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.small,
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
private fun Preview_AddressBookListScreen(
    @PreviewParameter(AddressBookListPreviewParameterProvider::class) scenario: AddressBookListPreviewScenario,
) {
    TangemThemePreviewRedesign {
        AddressBookListScreen(
            state = scenario.state,
            onBackClick = {},
        )
    }
}