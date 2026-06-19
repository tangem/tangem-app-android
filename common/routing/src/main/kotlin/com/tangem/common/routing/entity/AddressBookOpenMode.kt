package com.tangem.common.routing.entity

import kotlinx.serialization.Serializable

/** How the address book is opened — part of the navigation contract, carried by [com.tangem.common.routing.AppRoute.AddressBook]. */
@Serializable
sealed interface AddressBookOpenMode {

    @Serializable
    data object Default : AddressBookOpenMode

    @Serializable
    data class WithContactCreation(
        val address: String,
        val networkId: String,
    ) : AddressBookOpenMode
}