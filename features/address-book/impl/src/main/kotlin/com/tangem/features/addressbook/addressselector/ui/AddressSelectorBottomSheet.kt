package com.tangem.features.addressbook.addressselector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.impl.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun AddressSelectorBottomSheet(
    contact: MatchedContact,
    onAddressClick: (MatchedContact.ContactAddress) -> Unit,
    onDismiss: () -> Unit,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors3.bg.primary,
        title = {
            TangemTopBar(
                title = resourceReference(R.string.address_book_choose_address),
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = onDismiss,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = { AddressSelectorList(contact = contact, onAddressClick = onAddressClick) },
        footer = {
            TangemButton(
                onClick = onDismiss,
                text = resourceReference(R.string.common_cancel),
                variant = TangemButton.Variant.Secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        },
    )
}

@Composable
private fun AddressSelectorList(
    contact: MatchedContact,
    onAddressClick: (MatchedContact.ContactAddress) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors3.bg.secondary,
                shape = RoundedCornerShape(20.dp),
            )
            .verticalScroll(rememberScrollState()),
    ) {
        contact.entries.fastForEach { entry ->
            AddressRow(entry = entry, onClick = { onAddressClick(entry) })
        }
    }
}

@Composable
private fun AddressRow(entry: MatchedContact.ContactAddress, onClick: () -> Unit) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = onClick,
        startSlot = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Ident(entry.address),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        },
        titleSlot = {
            TangemRowText(
                text = entry.address,
                role = TangemRowTextRole.Title,
                overflow = TextOverflow.MiddleEllipsis,
            )
        },
        subtitleSlot = {
            TangemRowText(
                text = entry.networkName,
                role = TangemRowTextRole.Subtitle,
            )
        },
    )
}

@Composable
@Preview(showBackground = true, widthDp = 360)
private fun Preview_AddressSelectorList() {
    TangemThemePreviewRedesign {
        AddressSelectorList(
            contact = MatchedContact(
                contactId = "1",
                name = "Binance",
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Letter,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
                networkId = "ethereum",
                entries = persistentListOf(
                    MatchedContact.ContactAddress(
                        address = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",
                        memo = null,
                        networkName = "Ethereum",
                    ),
                    MatchedContact.ContactAddress(
                        address = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
                        memo = "12345",
                        networkName = "Ethereum",
                    ),
                ),
            ),
            onAddressClick = {},
        )
    }
}