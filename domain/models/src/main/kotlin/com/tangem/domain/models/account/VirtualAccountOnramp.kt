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

    /**
     * A VA on-ramp order has been submitted and is being provisioned (order status NEW/PROCESSING, or
     * COMPLETED before the ACCOUNT product instance appears). The bank-transfer entry point stays visible;
     * tapping it shows the "Preparing your banking details" bottom sheet. Transient — never persisted,
     * re-resolved on the next status fetch, cleared once the ACCOUNT instance appears or the order is canceled.
     */
    @Serializable
    data object Processing : VirtualAccountOnramp

    /**
     * VA product instance exists, but its bank credentials failed to load. The bank-transfer entry point
     * stays visible; tapping it surfaces a retryable "couldn't load banking details" error instead of the
     * requisites. Transient — never persisted, re-resolved on the next status fetch.
     */
    @Serializable
    data object BankCredentialsError : VirtualAccountOnramp
}