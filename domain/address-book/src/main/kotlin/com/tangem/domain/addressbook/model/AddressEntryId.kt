package com.tangem.domain.addressbook.model

import kotlinx.serialization.Serializable

/** Client-generated UUID v4 identifier of an [AddressEntry]. */
@Serializable
@JvmInline
value class AddressEntryId(val value: String)