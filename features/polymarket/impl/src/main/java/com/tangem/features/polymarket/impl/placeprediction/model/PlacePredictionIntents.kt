package com.tangem.features.polymarket.impl.placeprediction.model

import java.math.BigDecimal

/** What the place-prediction screens may ask the flow to do. */
internal interface PlacePredictionIntents {

    fun onAmountChange(value: String)

    fun onSlippageSelected(percent: BigDecimal)

    fun onQuoteRetryClick()

    fun onNextClick()

    fun onPlaceClick()

    fun onBackClick()

    fun onCloseClick()
}