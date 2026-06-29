package com.tangem.features.addressbook.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries the set of network ids confirmed on the SelectNetworks screen back to the AddAddress screen.
 *
 * The two screens live in independent model scopes, so a shared singleton holder hands the result over instead of
 * routing it through navigation. Only the "Done" action sets a result; the producer calls [setSelectedNetworkIds], the
 * consumer observes [selectedNetworkIds] and calls [clear] after applying it so it is not re-applied on resubscription.
 *
 * Mirrors [AddressBookResultHolder].
 */
@Singleton
internal class SelectNetworksResultHolder @Inject constructor() {

    val selectedNetworkIds: StateFlow<Set<String>?>
        field = MutableStateFlow<Set<String>?>(null)

    fun setSelectedNetworkIds(networkIds: Set<String>) {
        selectedNetworkIds.value = networkIds
    }

    fun clear() {
        selectedNetworkIds.value = null
    }
}