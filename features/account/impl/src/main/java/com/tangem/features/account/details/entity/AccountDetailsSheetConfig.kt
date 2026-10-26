package com.tangem.features.account.details.entity

internal sealed interface AccountDetailsSheetConfig {

    data object SupportedNetworks : AccountDetailsSheetConfig

    data class AboutSignatures(val threshold: Int, val total: Int) : AccountDetailsSheetConfig
}