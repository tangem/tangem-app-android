package com.tangem.features.addressbook.list.state.transformers.converter

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.addressbook.model.VerifiedContact
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.utils.converter.Converter

internal class DefaultContactConverter(
    private val onContactClick: (String) -> Unit,
) : Converter<VerifiedContact, ContactUM> {

    override fun convert(value: VerifiedContact): ContactUM {
        val contact = value.contact
        val name = contact.name.value
        return ContactUM(
            id = contact.id.value,
            walletId = contact.walletId.stringValue,
            name = name,
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.entries.firstOrNull { it.name == contact.icon }
                    ?: CryptoPortfolioIcon.Icon.Letter,
                color = CryptoPortfolioIcon.Color.entries.firstOrNull { it.name == contact.iconColor }
                    ?: CryptoPortfolioIcon.Color.Azure,
            ),
            networkAddressCount = contact.addressEntries.size,
            onClick = { onContactClick(contact.id.value) },
        )
    }
}