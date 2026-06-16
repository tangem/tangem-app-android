package com.tangem.domain.addressbook.model

/**
 * Outcome of verifying a [Contact]'s [AddressEntry]s against the wallet that signed them.
 *
 * @property valid   entries whose signature was produced by the wallet — these should be shown.
 * @property invalid entries that failed verification (tampered, signed by another wallet, or carrying
 *                   a missing/malformed signature) — these should be hidden.
 */
data class AddressEntriesVerification(
    val valid: List<AddressEntry>,
    val invalid: List<AddressEntry>,
) {
    val areAllInvalid: Boolean get() = valid.isEmpty() && invalid.isNotEmpty()
}