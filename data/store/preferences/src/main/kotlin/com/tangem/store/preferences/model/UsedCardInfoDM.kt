package com.tangem.store.preferences.model

internal data class UsedCardInfoDM(
    val cardId: String,
    val isScanned: Boolean = false,
    val isActivationStarted: Boolean = false,
    val isActivationFinished: Boolean = false,
)
