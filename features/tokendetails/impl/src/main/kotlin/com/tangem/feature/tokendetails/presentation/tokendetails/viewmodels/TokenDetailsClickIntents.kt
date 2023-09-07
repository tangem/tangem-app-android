package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import com.tangem.core.ui.components.transactions.intents.TxHistoryClickIntents

interface TokenDetailsClickIntents : TxHistoryClickIntents {

    fun onBackClick()

    fun onSendClick()

    fun onReceiveClick()

    fun onSellClick()

    fun onSwapClick()

    fun onDismissDialog()

    fun onHideClick()

    fun onHideConfirmed()

    fun onRefreshSwipe()
}