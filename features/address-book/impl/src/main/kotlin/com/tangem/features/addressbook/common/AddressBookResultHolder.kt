package com.tangem.features.addressbook.common

import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries an address confirmed on the AddAddress screen over to the EditContact screen.
 *
 * The two screens live in independent model scopes, so a shared singleton holder is used to hand the result over
 * instead of routing it through navigation/click intents. The producer calls [setConfirmedAddress]; the consumer
 * observes [confirmedAddress] and calls [clear] after applying the value so it is not re-applied on resubscription.
 */
@Singleton
internal class AddressBookResultHolder @Inject constructor() {

    val confirmedAddress: StateFlow<ConfirmedAddress?>
        field = MutableStateFlow<ConfirmedAddress?>(null)

    fun setConfirmedAddress(confirmed: ConfirmedAddress) {
        confirmedAddress.value = confirmed
    }

    fun clear() {
        confirmedAddress.value = null
    }
}

internal data class ConfirmedAddress(
    val address: ValidatedAddress,
    val replaces: String?,
)