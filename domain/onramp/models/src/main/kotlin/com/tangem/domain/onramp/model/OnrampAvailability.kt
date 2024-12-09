package com.tangem.domain.onramp.model

sealed interface OnrampAvailability {

    val country: OnrampCountry

    data class Available(override val country: OnrampCountry, val currency: OnrampCurrency) : OnrampAvailability
    data class NotSupported(override val country: OnrampCountry) : OnrampAvailability
    data class ConfirmResidency(override val country: OnrampCountry) : OnrampAvailability
}