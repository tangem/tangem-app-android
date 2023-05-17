package com.tangem.data.source.preferences.model

internal data class DataSourceUsedCardInfoOld(
    val cardId: String,
    val isScanned: Boolean = false,
    val isActivationStarted: Boolean = false,
)