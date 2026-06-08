package com.tangem.features.addressbook

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AddressBookComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, AddressBookComponent>

    data class Params(val predefinedAddress: String?)
}