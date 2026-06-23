package com.tangem.features.addressbook.list.state.transformers

import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.features.addressbook.list.ui.state.ContentMode
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the contacts list from the [matched] contacts. An empty result falls back to [AddressBookListUM.Empty] so the
 * user can still add a contact.
 */
internal class UpdateAddressBookListSelectionStateTransformer(
    private val matched: List<MatchedContact>,
    private val onAddContactClick: () -> Unit,
    private val onContactClick: (MatchedContact) -> Unit,
) : Transformer<AddressBookListUM> {

    override fun transform(prevState: AddressBookListUM): AddressBookListUM {
        return if (matched.isEmpty()) {
            AddressBookListUM.Empty(onAddClick = onAddContactClick)
        } else {
            AddressBookListUM.Content(
                contacts = matched.map { it.toContactUM() }.toImmutableList(),
                contentMode = ContentMode.Select,
            )
        }
    }

    private fun MatchedContact.toContactUM(): ContactUM = ContactUM(
        id = contactId,
        name = name,
        icon = icon,
        networkAddressCount = entries.size,
        onClick = { onContactClick(this) },
    )
}