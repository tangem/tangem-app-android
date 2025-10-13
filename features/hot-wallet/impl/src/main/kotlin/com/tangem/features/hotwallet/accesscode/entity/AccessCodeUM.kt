package com.tangem.features.hotwallet.accesscode.entity

import com.tangem.core.ui.components.fields.PinTextColor
import com.tangem.features.hotwallet.accesscode.ACCESS_CODE_LENGTH

internal data class AccessCodeUM(
    val accessCode: String,
    val accessCodeColor: PinTextColor,
    val onAccessCodeChange: (String) -> Unit,
    val isConfirmMode: Boolean,
    val buttonEnabled: Boolean,
    val buttonInProgress: Boolean,
    val onButtonClick: () -> Unit,
) {
    val accessCodeLength: Int = ACCESS_CODE_LENGTH
}