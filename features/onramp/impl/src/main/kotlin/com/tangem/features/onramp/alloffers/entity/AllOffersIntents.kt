package com.tangem.features.onramp.alloffers.entity

import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.features.onramp.mainv2.entity.OnrampOfferAdvantagesUM

internal interface AllOffersIntents {

    fun onPaymentMethodClicked(paymentMethodId: String)

    fun onBuyClick(quote: OnrampProviderWithQuote.Data, onrampOfferAdvantagesUM: OnrampOfferAdvantagesUM)

    fun onBackClicked()

    fun onRefresh()
}