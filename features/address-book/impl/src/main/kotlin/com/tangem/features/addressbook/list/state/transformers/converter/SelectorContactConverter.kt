package com.tangem.features.addressbook.list.state.transformers.converter

import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.utils.converter.Converter

internal class SelectorContactConverter(
    private val onPickContact: (MatchedContact) -> Unit,
) : Converter<MatchedContact, ContactUM> {

    override fun convert(value: MatchedContact): ContactUM = ContactUM(
        id = value.contactId,
        walletId = value.walletId,
        name = value.name,
        icon = value.icon,
        networkAddressCount = value.entries.size,
        onClick = { onPickContact(value) },
    )
}