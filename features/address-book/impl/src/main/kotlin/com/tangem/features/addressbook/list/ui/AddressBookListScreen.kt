package com.tangem.features.addressbook.list.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_20
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.common.ui.ContactRow
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.features.addressbook.list.ui.state.ContentMode
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun AddressBookListScreen(
    state: AddressBookListUM.Content,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(items = state.contacts, key = ContactUM::id) { contact ->
                ContactRow(contact = contact)
            }
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
private fun Preview_AddressBookListScreen() {
    TangemThemePreviewRedesign {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            AddressBookListScreen(
                state = AddressBookListUM.Content(
                    contacts = persistentListOf(
                        ContactUM(
                            id = "1",
                            name = "Binance",
                            icon = AccountIconUM.CryptoPortfolio(
                                value = CryptoPortfolioIcon.Icon.Letter,
                                color = CryptoPortfolioIcon.Color.Azure,
                            ),
                            networkAddressCount = 1,
                            onClick = {},
                        ),
                        ContactUM(
                            id = "2",
                            name = "Alice",
                            icon = AccountIconUM.CryptoPortfolio(
                                value = CryptoPortfolioIcon.Icon.Letter,
                                color = CryptoPortfolioIcon.Color.UFOGreen,
                            ),
                            networkAddressCount = 3,
                            onClick = {},
                        ),
                    ),
                    contentMode = ContentMode.Default(onAddClick = {}),
                ),
                onBackClick = {},
            )

            AddressBookListScreen(
                state = AddressBookListUM.Content(
                    contacts = persistentListOf(
                        ContactUM(
                            id = "1",
                            name = "Binance",
                            icon = AccountIconUM.CryptoPortfolio(
                                value = CryptoPortfolioIcon.Icon.Letter,
                                color = CryptoPortfolioIcon.Color.Azure,
                            ),
                            networkAddressCount = 1,
                            onClick = {},
                        ),
                        ContactUM(
                            id = "2",
                            name = "Alice",
                            icon = AccountIconUM.CryptoPortfolio(
                                value = CryptoPortfolioIcon.Icon.Letter,
                                color = CryptoPortfolioIcon.Color.UFOGreen,
                            ),
                            networkAddressCount = 3,
                            onClick = {},
                        ),
                    ),
                    contentMode = ContentMode.Select,
                ),
                onBackClick = {},
            )
        }
    }
}