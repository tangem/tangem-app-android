package com.tangem.domain.addressbook.model

import kotlinx.serialization.Serializable

/** Client-generated UUID v4 identifier of a [Contact]. */
@Serializable
@JvmInline
value class ContactId(val value: String)