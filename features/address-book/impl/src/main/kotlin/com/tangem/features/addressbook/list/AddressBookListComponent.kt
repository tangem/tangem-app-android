package com.tangem.features.addressbook.list

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

internal interface AddressBookListComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, AddressBookListComponent>

    data class Params(
        val onContactClick: (String) -> Unit,
        val onAddContactClick: () -> Unit,
    )
}