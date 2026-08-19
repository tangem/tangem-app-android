package com.tangem.features.polymarket.impl.placeprediction.model

import java.math.BigDecimal

/** What the place-prediction screens may ask the flow to do. */
internal interface PlacePredictionIntents {

    fun onAmountChange(value: String)

    fun onSlippageSelected(percent: BigDecimal)

    fun onSlippageClick()

    fun onAddFundsClick()

    fun onQuoteRetryClick()

    fun onNextClick()

    fun onPlaceClick()

    fun onPolymarketTermsClick()

    fun onTangemTermsClick()

    fun onBackClick()

    fun onCloseClick()
}