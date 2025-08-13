package com.tangem.features.disclaimer.impl.entity

internal data class DisclaimerUM(
    val url: String,
    val isTosAccepted: Boolean,
    val onAccept: () -> Unit,
    val popBack: () -> Unit,
)

internal object DummyDisclaimer {

    val state = DisclaimerUM(
        url = "https://tangem.com/tangem_tos.html",
        isTosAccepted = false,
        onAccept = {},
        popBack = {},
    )
}