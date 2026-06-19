package com.tangem.features.addressbook.common

import com.tangem.domain.addressbook.model.ContactId
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress

/**
 * Navigation/click contract that the container ([DefaultAddressBookComponent]) implements and passes down to its
 * children through [AddressBookChildFactory]. Keeping all cross-screen intents in one place removes the need for the
 * children to know about each other or about navigation.
 *
 * Result delivery (the confirmed address) is handled out-of-band by [AddressBookResultHolder], not by this contract.
 */
internal interface AddressBookClickIntents {

    fun onContactClick(contactId: ContactId)

    fun onAddContactClick()

    fun onEditContactBack()

    fun onAddAddressClick()

    fun onAddAddressBack()

    fun onAddressConfirmed(address: ValidatedAddress)
}