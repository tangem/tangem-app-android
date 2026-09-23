package com.tangem.features.polymarket.impl.placeprediction.model

import java.math.BigDecimal

internal interface PlacePredictionAmountIntents {

    fun onAmountChange(value: String)

    fun onSlippageClick()

    fun onAddFundsClick()

    fun onQuoteRetryClick()

    fun onNextClick()

    fun onBackClick()

    fun onCloseClick()
}

internal interface PlacePredictionSummaryIntents {

    fun onFeeClick()

    fun onQuoteRetryClick()

    fun onPlaceClick()

    fun onPolymarketTermsClick()

    fun onTangemTermsClick()

    fun onBackClick()

    fun onCloseClick()
}

internal interface PlacePredictionSlippageIntents {

    fun onSlippageValueChange(value: String)

    fun onSlippagePresetClick(percent: BigDecimal)

    fun onSlippageConfirmClick()

    fun onBackClick()
}

internal interface PlacePredictionStatusIntents {

    fun onToMainClick()

    fun onViewPositionClick()
}