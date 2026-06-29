package com.tangem.features.addressbook.block.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.features.addressbook.list.ui.state.ContactUM
import kotlinx.collections.immutable.ImmutableList

/** UI state of the Send contacts block. [Hidden] is rendered as nothing (no matching contacts / feature off). */
@Immutable
internal sealed interface ContactsBlockUM {

    data object Hidden : ContactsBlockUM

    data class Content(
        val shouldShowSeeAll: Boolean,
        val contacts: ImmutableList<ContactUM>,
        val onSeeAllClick: () -> Unit,
    ) : ContactsBlockUM
}