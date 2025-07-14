package com.tangem.features.walletconnect.transaction.entity.common

import kotlinx.coroutines.flow.StateFlow

internal interface WcCommonTransactionModel {

    val uiState: StateFlow<WcCommonTransactionUM?>

    fun dismiss()

    fun popBack()
}