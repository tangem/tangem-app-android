package com.tangem.features.tangempay.card.pin

import com.tangem.core.ui.extensions.TextReference

internal data class TangemPayChangePinUM(
    val pinCode: String,
    val error: TextReference?,
    val onPinCodeChange: (String) -> Unit,
    val submitButtonLoading: Boolean,
    val submitButtonEnabled: Boolean,
    val onSubmitClick: () -> Unit,
)