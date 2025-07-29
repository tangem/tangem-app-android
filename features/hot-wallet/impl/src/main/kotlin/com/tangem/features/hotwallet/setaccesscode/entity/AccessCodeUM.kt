package com.tangem.features.hotwallet.setaccesscode.entity

import com.tangem.features.hotwallet.setaccesscode.ACCESS_CODE_LENGTH

internal data class AccessCodeUM(
    val accessCode: String,
    val onAccessCodeChange: (String) -> Unit,
    val buttonEnabled: Boolean,
    val onButtonClick: () -> Unit,
) {
    val accessCodeLength: Int = ACCESS_CODE_LENGTH
}