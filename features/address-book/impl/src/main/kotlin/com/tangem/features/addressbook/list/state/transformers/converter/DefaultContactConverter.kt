package com.tangem.features.addressbook.list.state.transformers.converter

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.utils.converter.Converter

internal class DefaultContactConverter(
    private val onContactClick: (String) -> Unit,
) : Converter<Contact, ContactUM> {

    override fun convert(value: Contact): ContactUM {
        val name = value.name.value
        return ContactUM(
            id = value.id.value,
            walletId = value.walletId.stringValue,
            name = name,
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.entries.firstOrNull { it.name == value.icon }
                    ?: CryptoPortfolioIcon.Icon.Letter,
                color = CryptoPortfolioIcon.Color.entries.firstOrNull { it.name == value.iconColor }
                    ?: CryptoPortfolioIcon.Color.Azure,
            ),
            networkAddressCount = value.addresses.size,
            onClick = { onContactClick(value.id.value) },
        )
    }
}