package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.interactor.GetVerifiedContactsInteractor
import com.tangem.domain.addressbook.model.ContactAddressLookup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Emits the address → contact index built over the address books of every wallet on the device.
 *
 * All wallets are taken deliberately: an address saved only in another wallet's book is still shown by name, while the
 * wallet a transaction was made in takes precedence — the caller passes it to [ContactAddressLookup.resolve].
 */
class GetContactAddressLookupUseCase(
    private val getVerifiedContacts: GetVerifiedContactsInteractor,
) {

    operator fun invoke(): Flow<ContactAddressLookup> = getVerifiedContacts
        .getVerifiedContacts(query = "")
        .distinctUntilChanged()
        .map(ContactAddressLookup::of)
}