package com.tangem.domain.onramp.model

sealed interface OnrampAvailability {

    data object Available : OnrampAvailability
    data class NotSupported(val country: OnrampCountry) : OnrampAvailability
    data class ConfirmResidency(val country: OnrampCountry) : OnrampAvailability
}
