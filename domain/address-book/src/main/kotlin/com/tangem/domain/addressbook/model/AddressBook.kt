package com.tangem.domain.addressbook.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressBook(
    @SerialName("contacts")
    val contacts: List<Contact>,
)