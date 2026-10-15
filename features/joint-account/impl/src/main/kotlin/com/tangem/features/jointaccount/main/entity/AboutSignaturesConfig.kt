package com.tangem.features.jointaccount.main.entity

/**
 * Slot config for the "About signatures" sheet.
 *
 * @property threshold signatures required to execute an operation.
 * @property total     members of the account.
 */
internal data class AboutSignaturesConfig(
    val threshold: Int,
    val total: Int,
)