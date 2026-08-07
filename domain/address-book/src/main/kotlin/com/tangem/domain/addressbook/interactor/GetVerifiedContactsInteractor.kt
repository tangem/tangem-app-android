package com.tangem.domain.addressbook.interactor

import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.domain.addressbook.verification.ContactSignatureVerifier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetVerifiedContactsInteractor(
    private val getContacts: GetContactsUseCase,
    private val contactSignatureVerifier: ContactSignatureVerifier,
) {

    fun getVerifiedContacts(query: String, userWalletId: UserWalletId? = null): Flow<List<Contact>> {
        return getContacts(query, userWalletId).map { contacts ->
            contactSignatureVerifier.verifyContacts(contacts)
        }
    }
}