package com.tangem.features.addressbook.block.state.transformers

import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.block.ui.state.ContactsBlockUM
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/** Builds the Send contacts block from the network-matching contacts; an empty result hides the block. */
internal class UpdateContactsBlockStateTransformer(
    private val matched: List<MatchedContact>,
    private val onSeeAllClick: () -> Unit,
    private val onContactClick: (MatchedContact) -> Unit,
) : Transformer<ContactsBlockUM> {

    override fun transform(prevState: ContactsBlockUM): ContactsBlockUM {
        return if (matched.isEmpty()) {
            ContactsBlockUM.Hidden
        } else {
            ContactsBlockUM.Content(
                contacts = matched
                    .take(MAX_CONTACTS)
                    .map { it.toRowUM() }.toImmutableList(),
                onSeeAllClick = onSeeAllClick,
                shouldShowSeeAll = matched.size > MAX_CONTACTS,
            )
        }
    }

    private fun MatchedContact.toRowUM(): ContactUM = ContactUM(
        id = contactId,
        name = name,
        icon = icon,
        networkAddressCount = entries.size,
        onClick = { onContactClick(this) },
    )

    private companion object {
        const val MAX_CONTACTS = 5
    }
}