package com.tangem.features.addressbook.list.ui.state

import androidx.compose.runtime.Immutable

/** UI model of a single address-book contact row. Holds only what the list needs to render — no domain types. */
@Immutable
internal data class ContactUM(
    val id: String,
    val name: String,
    val addressCount: Int,
)