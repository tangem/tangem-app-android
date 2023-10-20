package com.tangem.tap.features.details.ui.appcurrency

internal interface AppCurrencySelectorIntents {

    fun onBackClick()

    fun onSearchClick()

    fun onSearchInputChange(input: String)

    fun onCurrencyClick(currency: AppCurrencySelectorState.Currency)

    fun onDismissSearchClick()
}