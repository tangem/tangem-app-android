package com.tangem.features.onramp.alloffers.entity

import com.tangem.domain.onramp.model.OnrampProviderWithQuote

interface AllOffersIntents {

    fun onPaymentMethodClicked(paymentMethodId: String)

    fun onBuyClick(quote: OnrampProviderWithQuote.Data)

    fun onBackClicked()

    fun onRefresh()
}