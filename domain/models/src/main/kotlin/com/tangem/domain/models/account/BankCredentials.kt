package com.tangem.domain.models.account

import kotlinx.serialization.Serializable

/**
 * Bank (fiat) credentials for a Virtual Account on-ramp — the wire/ACH requisites a user transfers funds to.
 *
 * Returned by `bff-v2/v1/account/bank-credentials/{product_instance_id}`. Sensitive data — kept transient
 * (never persisted in the local payment-account cache).
 */
@Serializable
data class BankCredentials(
    val type: String,
    val beneficiaryName: String,
    val beneficiaryAddress: String,
    val beneficiaryBankName: String,
    val beneficiaryBankAddress: String,
    val accountNumber: String,
    val routingNumber: String,
)