package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampProviderWithQuote

internal interface OnrampIntents {
    fun onAmountValueChanged(value: String)
    fun openSettings()
    fun openCurrenciesList()
    fun onBuyClick(
        quote: OnrampProviderWithQuote.Data,
        onrampOfferAdvantagesUM: OnrampOfferAdvantagesUM,
        categoryUM: OnrampOfferCategoryUM,
    )
    fun openProviders()
    fun onRefresh()
}