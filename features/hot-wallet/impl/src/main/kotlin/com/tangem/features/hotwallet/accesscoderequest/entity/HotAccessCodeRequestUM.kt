package com.tangem.features.hotwallet.accesscoderequest.entity

internal data class HotAccessCodeRequestUM(
    val isShown: Boolean = false,
    val accessCode: String = "",
    val wrongAccessCode: Boolean = false,
    val useBiometricVisible: Boolean = true,
    val useBiometricClick: () -> Unit = {},
    val onAccessCodeChange: (String) -> Unit = {},
    val onDismiss: () -> Unit = {},
)