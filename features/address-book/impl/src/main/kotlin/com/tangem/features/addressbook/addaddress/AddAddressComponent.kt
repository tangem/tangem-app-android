package com.tangem.features.addressbook.addaddress

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.addressbook.editcontact.contract.ValidatedAddress

internal interface AddAddressComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, AddAddressComponent>

    data class Params(
        val onBackClick: () -> Unit,
        val onConfirm: (ValidatedAddress) -> Unit,
    )
}