package com.tangem.domain.models.account

import kotlinx.serialization.Serializable

/**
 * Virtual Account (Visa on-ramp) availability for a payment account — VA MVP0 (TWI-1638).
 *
 * Computed in the payment-account fetcher and surfaced on [PaymentAccountStatusValue.Loaded].
 */
@Serializable
sealed interface VirtualAccountOnramp {

    /** No VA product instance yet, but the wallet is eligible to add funds (channel `VISA_VIRTUAL_ACCOUNT`). */
    @Serializable
    data object Eligible : VirtualAccountOnramp

    /**
     * A VA product instance exists. Its fiat [BankCredentials] are fetched on demand by the deposit screen
     * (via [productInstanceId]) rather than eagerly by the fetcher.
     */
    @Serializable
    data class Available(
        val productInstanceId: String,
    ) : VirtualAccountOnramp

    /**
     * A VA on-ramp order has been submitted and is being provisioned (order status NEW/PROCESSING, or
     * COMPLETED before the ACCOUNT product instance appears). The bank-transfer entry point stays visible;
     * tapping it shows the "Preparing your banking details" bottom sheet. Transient — never persisted,
     * re-resolved on the next status fetch, cleared once the ACCOUNT instance appears or the order is canceled.
     */
    @Serializable
    data object Processing : VirtualAccountOnramp
}