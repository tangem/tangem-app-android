package com.tangem.domain.models.account

import kotlinx.serialization.Serializable

/**
 * Virtual Account (Visa on-ramp) availability for a payment account — VA MVP0 (TWI-1638).
 *
 * Computed in the payment-account fetcher and surfaced on [PaymentAccountStatusValue.Loaded].
 * Transient: [Available.bankCredentials] is never persisted in the local cache.
 */
@Serializable
sealed interface VirtualAccountOnramp {

    /** No VA product instance yet, but the wallet is eligible to add funds (channel `VISA_VIRTUAL_ACCOUNT`). */
    @Serializable
    data object Eligible : VirtualAccountOnramp

    /** VA product instance exists; [bankCredentials] are the fiat requisites for the bank-transfer top-up. */
    @Serializable
    data class Available(
        val productInstanceId: String,
        val bankCredentials: BankCredentials,
    ) : VirtualAccountOnramp
}