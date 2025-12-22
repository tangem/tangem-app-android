package com.tangem.features.hotwallet.accesscode.entity

import com.tangem.core.ui.components.fields.PinTextColor
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.features.hotwallet.accesscode.ACCESS_CODE_LENGTH

internal data class AccessCodeUM(
    val accessCode: String,
    val accessCodeColor: PinTextColor,
    val isLoading: Boolean,
    val onAccessCodeChange: (String) -> Unit,
    val isConfirmMode: Boolean,
    val requestFocus: StateEvent<Unit> = consumedEvent(),
) {
    val accessCodeLength: Int = ACCESS_CODE_LENGTH
}