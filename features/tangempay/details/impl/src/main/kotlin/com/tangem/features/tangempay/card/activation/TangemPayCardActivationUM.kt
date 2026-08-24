package com.tangem.features.tangempay.card.activation

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.pay.model.CARD_ACTIVATION_LAST_DIGITS_LENGTH

@Immutable
internal data class TangemPayCardActivationUM(
    val lastDigits: String,
    val cardImageUrl: String?,
    val hint: TextReference,
    val isHintError: Boolean,
    val isLoading: Boolean,
    val onLastDigitsChange: (String) -> Unit,
    val onContinueClick: () -> Unit,
    val onCloseClick: () -> Unit,
) {

    val isContinueEnabled: Boolean
        get() = lastDigits.length == CARD_ACTIVATION_LAST_DIGITS_LENGTH
}