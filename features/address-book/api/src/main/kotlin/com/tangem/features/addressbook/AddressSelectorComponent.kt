package com.tangem.features.addressbook

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

/**
 * Bottom sheet shown when a picked contact has more than one address in the target network. Lets the user choose a
 * concrete address; the chosen one is returned via [Params.onAddressSelected] as a [SelectedContact].
 */
interface AddressSelectorComponent : ComposableBottomSheetComponent {

    interface Factory : ComponentFactory<Params, AddressSelectorComponent>

    data class Params(
        val contact: MatchedContact,
        val onAddressSelected: (SelectedContact) -> Unit,
        val onDismiss: () -> Unit,
    )
}