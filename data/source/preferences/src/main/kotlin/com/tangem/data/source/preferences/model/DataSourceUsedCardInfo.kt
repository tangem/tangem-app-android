package com.tangem.data.source.preferences.model

internal data class DataSourceUsedCardInfo(
    val cardId: String,
    val isScanned: Boolean = false,
    val isActivationStarted: Boolean = false,
    val isActivationFinished: Boolean = false,
)