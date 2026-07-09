package com.tangem.features.addressbook

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Address-book analytics that are triggered from outside the address-book feature (i.e. from the Send flow).
 *
 * The [com.tangem.features.addressbook.analytics] events are internal to the address-book impl module, so the Send
 * feature cannot construct them directly — it reports the address-book funnel through this contract instead.
 */
interface AddressBookSendAnalytics {

    /**
     * Fired when the recipient address (and memo, if any) picked from the address book has been substituted into the
     * Send form — the final step of choosing a recipient from the book.
     *
     * @param walletId the current (sending) wallet
     * @param contactId id of the picked contact
     */
    fun onAddressSubstitutedInSend(walletId: UserWalletId, contactId: String)
}