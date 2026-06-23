package com.tangem.features.addressbook.block.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.block.ui.state.ContactsBlockUM
import com.tangem.features.addressbook.common.ui.ContactRow
import com.tangem.features.addressbook.list.ui.state.ContactUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ContactsBlock(state: ContactsBlockUM, modifier: Modifier = Modifier) {
    if (state !is ContactsBlockUM.Content) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        Header(onSeeAllClick = state.onSeeAllClick, shouldShowSeeAll = state.shouldShowSeeAll)
        state.contacts.forEach { contact ->
            ContactRow(contact = contact)
        }
    }
}

@Composable
private fun Header(onSeeAllClick: () -> Unit, shouldShowSeeAll: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 4.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.address_book_title),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            modifier = Modifier.weight(1f),
        )
        if (shouldShowSeeAll) {
            Text(
                text = stringResourceSafe(R.string.common_view_all),
                color = TangemTheme.colors3.text.brand,
                style = TangemTheme.typography3.caption.medium,
                modifier = Modifier.clickable(onClick = onSeeAllClick),
            )
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
private fun Preview_ContactsBlock() {
    TangemThemePreviewRedesign {
        ContactsBlock(
            state = ContactsBlockUM.Content(
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
                onSeeAllClick = {},
                shouldShowSeeAll = true,
            ),
        )
    }
}