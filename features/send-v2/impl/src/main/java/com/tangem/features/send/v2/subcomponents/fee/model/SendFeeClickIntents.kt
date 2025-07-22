package com.tangem.features.send.v2.subcomponents.fee.model

import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType

internal interface SendFeeClickIntents {

    fun feeReload()

    fun onFeeSelectorClick(feeType: FeeType)

    fun onCustomFeeValueChange(index: Int, value: String)

    fun onNonceChange(value: String)

    fun onReadMoreClick()

    fun onNextClick()
}