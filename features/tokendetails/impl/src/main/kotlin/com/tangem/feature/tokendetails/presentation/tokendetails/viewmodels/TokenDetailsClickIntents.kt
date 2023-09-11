package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import com.tangem.core.ui.components.transactions.intents.TxHistoryClickIntents

interface TokenDetailsClickIntents : TxHistoryClickIntents {

    fun onBackClick()

    fun onMoreClick()

    fun onSendClick()

    fun onReceiveClick()

    fun onSellClick()

    fun onSwapClick()
}