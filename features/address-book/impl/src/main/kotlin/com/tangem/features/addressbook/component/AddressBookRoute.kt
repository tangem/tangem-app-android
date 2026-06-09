package com.tangem.features.addressbook.component

import kotlinx.serialization.Serializable

@Serializable
internal sealed class AddressBookRoute {

    @Serializable
    data object List : AddressBookRoute()
}