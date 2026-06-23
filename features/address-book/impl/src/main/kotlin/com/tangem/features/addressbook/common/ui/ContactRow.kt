package com.tangem.features.addressbook.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.list.ui.state.ContactUM

@Composable
internal fun ContactRow(contact: ContactUM) {
    TangemRow(
        onClick = contact.onClick,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        contentLead = TangemRowContentLead.Start,
        startSlot = {
            AccountIcon(
                name = stringReference(contact.name),
                icon = contact.icon,
                size = AccountIconSize.Contact,
            )
        },
        titleSlot = {
            TangemRowText(
                text = contact.name,
                role = TangemRowTextRole.Title,
            )
        },
        subtitleSlot = {
            TangemRowText(
                text = pluralStringResourceSafe(
                    R.plurals.address_book_addresses,
                    contact.networkAddressCount,
                    contact.networkAddressCount,
                ),
                role = TangemRowTextRole.Subtitle,
            )
        },
    )
}

@Composable
@Preview(showBackground = true, widthDp = 360)
private fun Preview_ContactRow() {
    TangemThemePreviewRedesign {
        ContactRow(
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
        )
    }
}