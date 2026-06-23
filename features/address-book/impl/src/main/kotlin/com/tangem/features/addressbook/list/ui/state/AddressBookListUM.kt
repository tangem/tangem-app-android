package com.tangem.features.addressbook.list.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * UI state of the contacts list. The list itself is the same however the address book was opened — it is either
 * [Empty] or shows [Content]. How the address book was opened (browse vs. pick a recipient) only changes what a
 * contact tap does, which is captured by [ContactUM.onClick], not by a separate state.
 */
@Immutable
internal sealed interface AddressBookListUM {

    data class Empty(val onAddClick: () -> Unit) : AddressBookListUM

    data class Content(
        val contacts: ImmutableList<ContactUM>,
        val contentMode: ContentMode,
    ) : AddressBookListUM
}

@Immutable
internal sealed interface ContentMode {

    data class Default(val onAddClick: () -> Unit) : ContentMode

    data object Select : ContentMode
}