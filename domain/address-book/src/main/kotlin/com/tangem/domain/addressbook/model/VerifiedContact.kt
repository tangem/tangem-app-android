package com.tangem.domain.addressbook.model

/**
 * @property contact        the contact carrying only the entries whose signatures verified against the
 *                          wallet — what should be shown to the user.
 * @property invalidEntries entries that failed verification (tampered, signed by another wallet, or
 *                          malformed). Hidden from the UI but kept for analytics.
 */
data class VerifiedContact(
    val contact: Contact,
    val invalidEntries: List<AddressEntry>,
)