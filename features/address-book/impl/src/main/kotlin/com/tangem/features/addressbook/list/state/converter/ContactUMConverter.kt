package com.tangem.features.addressbook.list.state.converter

import com.tangem.domain.addressbook.model.Contact
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.utils.converter.Converter

/**
 * Maps a domain [Contact] to its UI representation [ContactUM].
 *
 * TODO AddressBook ([REDACTED_TASK_KEY]): wire into [com.tangem.features.addressbook.list.model.AddressBookListModel] when the contacts list
 *  is loaded from the repository and the [com.tangem.features.addressbook.list.ui.state.AddressBookListUM.AddressList]
 *  screen is implemented.
 */
internal class ContactUMConverter : Converter<Contact, ContactUM> {

    override fun convert(value: Contact): ContactUM = ContactUM(
        id = value.id.value,
        name = value.name.value,
        addressCount = value.addressEntries.size,
    )
}