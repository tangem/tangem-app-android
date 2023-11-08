package com.tangem.datasource.local.card

data class UsedCardInfo(
    val cardId: String,
    val isScanned: Boolean = false,
    val isActivationStarted: Boolean = false,
    val isActivationFinished: Boolean = false,
)