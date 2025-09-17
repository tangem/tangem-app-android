package com.tangem.features.onramp.mainv2.entity

import com.tangem.domain.onramp.model.OnrampProviderWithQuote

internal interface OnrampV2Intents {
    fun onAmountValueChanged(value: String)
    fun openSettings()
    fun openCurrenciesList()
    fun onBuyClick(quote: OnrampProviderWithQuote.Data, onrampOfferAdvantagesUM: OnrampOfferAdvantagesUM)
    fun openProviders()
    fun onRefresh()
    fun onContinueClick()
}