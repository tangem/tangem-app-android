package com.tangem.features.addressbook

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.StateFlow

/**
 * The contacts block shown on the Send address-entry screen, below the "recent" block. Lists up to five contacts that
 * have an address in [Params.network], filtered live by [Params.queryFlow] (the recipient-input text, matched against
 * contact name or address). Hidden when there are no matching contacts.
 */
interface AddressBookContactsBlockComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, AddressBookContactsBlockComponent>

    /**
     * @property userWalletId the sending wallet whose address book is shown
     * @property network      the current send network; only contacts with an address in this network are shown
     * @property queryFlow    the live recipient-input text used to filter the block
     * @property onContactClick invoked with the tapped contact and its network-matching entries; the host decides
     *  whether to apply it directly (single entry) or open the address selector (multiple entries)
     * @property onSeeAllClick invoked when the user taps "See all" to open the full address book in selection mode
     */
    data class Params(
        val userWalletId: UserWalletId,
        val network: Network,
        val queryFlow: StateFlow<String>,
        val onContactClick: (MatchedContact) -> Unit,
        val onSeeAllClick: () -> Unit,
    )
}