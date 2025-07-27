package com.tangem.features.hotwallet.accesscode.confirm.entity

import com.tangem.features.hotwallet.accesscode.ACCESS_CODE_LENGTH

internal data class ConfirmAccessCodeUM(
    val accessCode: String,
    val onAccessCodeChange: (String) -> Unit,
    val buttonEnabled: Boolean,
    val onConfirm: () -> Unit,
) {
    val accessCodeLength: Int = ACCESS_CODE_LENGTH
}