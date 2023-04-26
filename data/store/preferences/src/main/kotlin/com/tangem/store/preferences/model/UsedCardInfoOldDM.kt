package com.tangem.store.preferences.model

internal data class UsedCardInfoOldDM(
    val cardId: String,
    val isScanned: Boolean = false,
    val isActivationStarted: Boolean = false,
)
