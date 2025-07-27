package com.tangem.features.hotwallet.accesscode.set.entity

import com.tangem.features.hotwallet.accesscode.ACCESS_CODE_LENGTH

internal data class SetAccessCodeUM(
    val accessCode: String,
    val onAccessCodeChange: (String) -> Unit,
    val buttonEnabled: Boolean,
    val onContinue: () -> Unit,
) {
    val accessCodeLength: Int = ACCESS_CODE_LENGTH
}