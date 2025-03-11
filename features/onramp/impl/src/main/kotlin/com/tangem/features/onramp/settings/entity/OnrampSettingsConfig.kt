package com.tangem.features.onramp.settings.entity

import kotlinx.serialization.Serializable

@Serializable
sealed class OnrampSettingsConfig {

    data object SelectCountry : OnrampSettingsConfig()
}