package com.tangem.features.addressbook.list.state.transformers.converter

import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.portfolioIcon
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.utils.converter.Converter

internal class DefaultContactConverter(
    private val onContactClick: (String) -> Unit,
) : Converter<Contact, ContactUM> {

    override fun convert(value: Contact): ContactUM {
        return ContactUM(
            id = value.id.value,
            walletId = value.walletId.stringValue,
            name = value.name.value,
            icon = CryptoPortfolioIconConverter.convert(value.portfolioIcon()),
            networkAddressCount = value.addresses.size,
            onClick = { onContactClick(value.id.value) },
        )
    }
}