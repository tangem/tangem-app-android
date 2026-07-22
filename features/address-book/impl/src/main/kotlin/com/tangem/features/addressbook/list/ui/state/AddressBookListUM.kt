package com.tangem.features.addressbook.list.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.search.TangemSearch
import kotlinx.collections.immutable.ImmutableList

/**
 * UI state of the contacts list. The list itself is the same however the address book was opened — it is either
 * [Empty] (no contacts at all) or shows [Content]. How the address book was opened (browse vs. pick a recipient) only
 * changes what a contact tap does, which is captured by [ContactUM.onClick] and [Content.contentMode].
 */
@Immutable
internal sealed interface AddressBookListUM {

    /** Initial state while the address books are being (re-)synced on open — rendered as shimmer placeholders. */
    data object Loading : AddressBookListUM

    /**
     * A stored book uses a contract version newer than this build supports, so it can be neither read nor
     * safely edited. The screen shows an "update the app" stub instead of the list — no contacts, no add button.
     */
    data object Incompatible : AddressBookListUM

    data class Empty(val onAddClick: () -> Unit) : AddressBookListUM

    /**
     * @property searchBar      always shown so the user can filter contacts across all wallets.
     * @property chips          wallet filter chips (`All` + a chip per matching wallet); empty means the row is hidden.
     * @property contacts       contacts for the currently selected chip; empty together with [isNothingFound] = true.
     * @property isNothingFound true when the active search matched nothing — show the "no results" stub instead of the
     *                          list (the search bar stays visible so the query can be edited).
     */
    data class Content(
        val searchBar: TangemSearch.State,
        val chips: ImmutableList<AddressBookChipUM>,
        val contacts: ImmutableList<ContactUM>,
        val isNothingFound: Boolean,
        val contentMode: ContentMode,
    ) : AddressBookListUM
}

@Immutable
internal sealed interface ContentMode {

    data class Default(val onAddClick: () -> Unit) : ContentMode

    data object Select : ContentMode
}