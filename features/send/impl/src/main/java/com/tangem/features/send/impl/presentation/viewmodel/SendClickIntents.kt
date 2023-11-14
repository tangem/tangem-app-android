package com.tangem.features.send.impl.presentation.viewmodel

interface SendClickIntents {

    fun onNextClick()

    fun onPrevClick()

    fun onAmountValueChange(value: String)

    fun onCurrencyChangeClick(isFiat: Boolean)

    fun onMaxValueClick()
}