package com.tangem.features.onramp.main.entity

interface OnrampIntents {
    fun onAmountValueChanged(value: String)
    fun openSettings()
    fun openCurrenciesList()
    fun onBuyClick()
}