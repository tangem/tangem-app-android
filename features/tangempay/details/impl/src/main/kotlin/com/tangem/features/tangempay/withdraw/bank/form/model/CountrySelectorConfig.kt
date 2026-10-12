package com.tangem.features.tangempay.withdraw.bank.form.model

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CountrySelectorConfig {

    val field: BankWithdrawFormField

    @Serializable
    data class Country(
        override val field: BankWithdrawFormField,
        val selectedCountryCode: String? = null,
        val selectedRegionCode: String? = null,
    ) : CountrySelectorConfig

    @Serializable
    data class Region(
        override val field: BankWithdrawFormField,
        val countryCode: String,
        val selectedRegionCode: String? = null,
    ) : CountrySelectorConfig
}