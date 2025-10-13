package com.tangem.features.send.v2.sendnft.confirm.model

internal interface NFTSendConfirmClickIntents {

    fun showEditDestination()

    fun onSendClick()

    fun onExploreClick()

    fun onShareClick()

    fun onFailedTxEmailClick(errorMessage: String)
}