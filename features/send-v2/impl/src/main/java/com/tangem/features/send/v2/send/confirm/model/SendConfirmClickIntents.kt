package com.tangem.features.send.v2.send.confirm.model

internal interface SendConfirmClickIntents {

    fun showEditDestination()

    fun showEditAmount()

    fun showEditFee()

    fun onSendClick()

    fun onExploreClick()

    fun onShareClick()

    fun onFailedTxEmailClick(errorMessage: String)
}