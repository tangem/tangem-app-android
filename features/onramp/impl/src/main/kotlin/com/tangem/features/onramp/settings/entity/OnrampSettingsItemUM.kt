package com.tangem.features.onramp.settings.entity

sealed class OnrampSettingsItemUM {
    data class Residence(
        val countryName: String = "",
        val flagUrl: String = "",
        val onClick: () -> Unit,
    ) : OnrampSettingsItemUM()
}