package com.tangem.features.send.api.subcomponents.feeSelector.params

enum class SponsoredFeeAvailability {
    Unavailable,
    Available,

    /** The user cannot pay the fee themselves, so the sponsored fee is selected unless they pick otherwise. */
    Preferred,
}