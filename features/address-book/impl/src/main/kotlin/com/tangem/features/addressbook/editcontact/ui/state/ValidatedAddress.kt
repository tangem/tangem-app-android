package com.tangem.features.addressbook.editcontact.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * A recipient address validated on the AddAddress screen, together with the networks it resolves to.
 *
 * A single address can belong to several networks (e.g. the same address across EVM chains), so it carries a list of
 * [networkIds]. This is the in-progress (pre-save) representation accumulated in [EditContactUM]; the [networkIds] are
 * used to rebuild the domain `AddressEntry`s when the contact is persisted.
 */
@Immutable
data class ValidatedAddress(
    val address: String,
    val networkIds: ImmutableList<String>,
)