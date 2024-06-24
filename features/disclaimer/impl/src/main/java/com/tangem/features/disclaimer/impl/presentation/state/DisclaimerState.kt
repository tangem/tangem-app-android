package com.tangem.features.disclaimer.impl.presentation.state

internal data class DisclaimerState(
    val url: String,
    val isTosAccepted: Boolean,
    val onAccept: (Boolean) -> Unit,
)

internal object DummyDisclaimer {

    val state = DisclaimerState(
        url = "https://tangem.com/tangem_tos.html",
        isTosAccepted = false,
        onAccept = {},
    )
}