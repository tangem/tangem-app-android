package com.tangem.features.hotwallet.accesscoderequest.entity

import com.tangem.core.ui.components.fields.PinTextColor
import com.tangem.core.ui.extensions.TextReference

internal data class HotAccessCodeRequestUM(
    val isShown: Boolean = false,
    val accessCode: String = "",
    val accessCodeColor: PinTextColor = PinTextColor.Primary,
    val wrongAccessCodeText: TextReference? = null,
    val useBiometricVisible: Boolean = true,
    val useBiometricClick: () -> Unit = {},
    val onAccessCodeChange: (String) -> Unit = {},
    val onDismiss: () -> Unit = {},
)